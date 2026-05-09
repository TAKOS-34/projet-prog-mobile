import { Injectable, OnModuleInit } from '@nestjs/common';
import * as admin from 'firebase-admin';
import { join } from 'path';
import { PrismaService } from 'src/prisma/prisma.service';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { NotificationInfos } from './dto/notificationList.dto';
import { CdnService } from 'src/cdn/cdn.service';
import { UserSession } from 'src/utils/dto/userSession.dto';
import { NotificationType } from '@prisma/client';
import { UserFollowingList } from './dto/userFollowingList.dto';
import { getNextCursor } from 'src/utils/paginator/paginate.util';

@Injectable()
export class NotificationService implements OnModuleInit {
    onModuleInit() {
        admin.initializeApp({
            credential: admin.credential.cert(
                join(process.cwd(), 'firebase-auth.json'),
            ),
        });
    }

    constructor(
        private readonly prisma: PrismaService,
        private readonly cdn: CdnService
    ) {}



    async updateFcmToken(fcmToken: string, userId: number): Promise<ResponseMessage> {
        await this.prisma.fcmToken.upsert({
            where: { fcmToken },
            update: { userId },
            create: { fcmToken, userId }
        });

        return { status: true, message: 'Fcm Token updated' };
    }



    async getNotifications(limit: number, user: UserSession, cursor?: number): Promise<NotificationInfos> {
        const take = Math.min(Math.max(limit, 1), 50);

        const notifications = await this.prisma.notification.findMany({
            take,
            skip: cursor ? 1 : 0,
            ...(cursor ? { cursor: { id: cursor } } : {}),
            where: { userId: user.id },
            orderBy: [{ creationDate: 'desc' }, { id: 'desc' }],
            include: {
                targetUser: { select: { id: true, username: true, avatar: true } },
                post: { select: { imageExt: true  } },
                group: { select: { id: true, name: true, avatar: true } },
                tag: { select: { id: true, name: true } },
                localisation: { select: { id: true, name: true } }
            }
        });

        return {
            notifications: notifications.map(n => ({
                id: n.id,
                type: n.type,
                creationDate: n.creationDate,
                isRead: n.isRead,

                postId: n.targetPostId ?? undefined,
                postImage: (n.targetPostId && n.post?.imageExt) ? this.cdn.getPostUrl(n.targetPostId, n.post.imageExt) : undefined,

                targetUserId: n.targetUser?.id,
                targetUsername: n.targetUser?.username,
                targetUserAvatar: n.targetUser?.avatar ? this.cdn.getAvatarUrl(n.targetUser.avatar) : this.cdn.getAvatarUrl(null),

                groupId: n.targetGroupId ?? undefined,
                groupName: n.group?.name,
                groupAvatar: n.group?.avatar ? this.cdn.getGroupAvatarUrl(n.group.avatar) : undefined,

                tagId: n.tag?.id,
                tagName: n.tag?.name,

                localisationId: n.localisation?.id,
                localisationName: n.localisation?.name
            })),
            nextCursor: getNextCursor(notifications, limit)
        };
    }



    async getUserFollowing(user: UserSession): Promise<UserFollowingList[]> {
        const [users, groups, tags, localisations] = await Promise.all([
            this.prisma.userFollow.findMany({
                where: { followerId: user.id },
                select: {
                    followingId: true,
                    following: { select: { username: true, avatar: true } }
                }
            }),
            this.prisma.groupFollow.findMany({
                where: { followerId: user.id },
                select: {
                    groupId: true,
                    following: { select: { name: true, avatar: true } }
                }
            }),
            this.prisma.tagFollow.findMany({
                where: { followerId: user.id },
                select: { tagId: true, following: { select: { name: true } } }
            }),
            this.prisma.localisationFollow.findMany({
                where: { followerId: user.id },
                select: { localisationId: true, following: { select: { name: true } } }
            })
        ]);

        return [
            ...users.map(u => ({
                type: 'user',
                targetUserId: u.followingId,
                targetUsername: u.following.username,
                targetUserAvatar: this.cdn.getAvatarUrl(u.following.avatar)
            })),
            ...groups.map(g => ({
                type: 'group',
                targetGroupId: g.groupId,
                targetGroupName: g.following.name,
                targetGroupAvatar: this.cdn.getGroupAvatarUrl(g.following.avatar)
            })),
            ...tags.map(t => ({
                type: 'tag',
                targetTagId: t.tagId,
                targetTagName: t.following.name
            })),
            ...localisations.map(l => ({
                type: 'localisation',
                targetLocalisationId: l.localisationId,
                targetLocalisationName: l.following.name
            }))
        ];
    }



    async getNewNotificationNumber(user: UserSession): Promise<number> {
        return (await this.prisma.notification.findMany({ where: { isRead: false, userId: user.id } })).length;
    }



    async markAsRead(notificationsId: number[], user: UserSession): Promise<ResponseMessage> {
        await this.prisma.notification.updateMany({
            where: { userId: user.id, isRead: false, id: { in: notificationsId } },
            data: { isRead: true }
        });

        return { status: true, message: 'Notifications marked as read' };
    }



    async notifyNewPostUser(userId: number, postId: string, postTitle: string, groupId?: number): Promise<void> {
        let group = null;
        if (groupId) {
            group = await this.prisma.group.findUnique({
                where: { id: groupId },
                select: { isGroupPrivate: true }
            });
        }

        const followers = await this.prisma.userFollow.findMany({
            where: {
                followingId: userId,
                ...(group?.isGroupPrivate ? {
                    follower: {
                        memberOf: { some: { groupId } }
                    }
                } : {})
            },
            select: { followerId: true }
        });

        const recipientIds = followers.map(f => f.followerId);
        if (recipientIds.length === 0) return;

        await this.prisma.notification.createMany({
            data: recipientIds.map(id => ({
                targetUserId: userId,
                userId: id,
                type: NotificationType.NEW_POST_USER,
                targetPostId: postId
            }))
        });

        const tokenRows = await this.prisma.fcmToken.findMany({
            where: { userId: { in: recipientIds } },
            select: { fcmToken: true }
        });

        const fcmTokens = [...new Set(tokenRows.map(r => r.fcmToken))];
        if (fcmTokens.length === 0) return;

        const response = await admin.messaging().sendEachForMulticast({
            tokens: fcmTokens,
            notification: {
                title: 'Nouvelle publication ! ✨',
                body: `Un utilisateur que vous suivez vient de publier : "${postTitle}"`,
            },
            data: {
                type: NotificationType.NEW_POST_USER,
                postId: postId,
                postTitle: postTitle
            },
        });

        await this.cleanupTokens(fcmTokens, response);
    }



    async notifyNewPostGroup(groupId: number, userId: number, postId: string, postTitle: string): Promise<void> {
        const followers = await this.prisma.groupFollow.findMany({
            where: {
                groupId,
                followerId: { not: userId }
            },
            select: { followerId: true }
        });

        const recipientIds = followers.map(f => f.followerId);
        if (recipientIds.length === 0) return;

        await this.prisma.notification.createMany({
            data: recipientIds.map(id => ({
                targetUserId: userId,
                userId: id,
                type: NotificationType.NEW_POST_GROUP,
                targetPostId: postId,
                targetGroupId: groupId
            }))
        });

        const tokenRows = await this.prisma.fcmToken.findMany({
            where: { userId: { in: recipientIds } },
            select: { fcmToken: true }
        });

        const fcmTokens = [...new Set(tokenRows.map(r => r.fcmToken))];
        if (fcmTokens.length === 0) return;

        const response = await admin.messaging().sendEachForMulticast({
            tokens: fcmTokens,
            notification: {
                title: 'Nouveau post dans votre groupe ! 👥',
                body: `Une nouvelle publication a été ajoutée : "${postTitle}"`,
            },
            data: {
                type: NotificationType.NEW_POST_GROUP,
                postId: postId,
                postTitle: postTitle,
                groupId: groupId.toString()
            },
        });

        await this.cleanupTokens(fcmTokens, response);
    }



    async notifyNewPostTags(tags: string[], userId: number, postId: string, postTitle: string, groupId?: number): Promise<void> {
        const tagRecords = await this.prisma.tag.findMany({
            where: { name: { in: tags } },
            select: { id: true, name: true }
        });
        if (tagRecords.length === 0) return;

        const tagIds = tagRecords.map(t => t.id);

        let group = null;
        if (groupId) {
            group = await this.prisma.group.findUnique({
                where: { id: groupId },
                select: { isGroupPrivate: true }
            });
        }

        const tagFollowers = await this.prisma.tagFollow.findMany({
            where: {
                tagId: { in: tagIds },
                followerId: { not: userId },
                ...(group?.isGroupPrivate ? {
                    follower: {
                        memberOf: { some: { groupId } }
                    }
                } : {})
            },
            select: { followerId: true, tagId: true }
        });

        if (tagFollowers.length === 0) return;

        const recipientIds = [...new Set(tagFollowers.map(f => f.followerId))];

        await this.prisma.notification.createMany({
            data: tagFollowers.map(f => ({
                targetUserId: userId,
                userId: f.followerId,
                type: NotificationType.NEW_POST_TAG,
                targetPostId: postId,
                targetTagId: f.tagId
            }))
        });

        const tokenRows = await this.prisma.fcmToken.findMany({
            where: { userId: { in: recipientIds } },
            select: { fcmToken: true }
        });

        const fcmTokens = [...new Set(tokenRows.map(r => r.fcmToken))];
        if (fcmTokens.length === 0) return;

        const formattedTags = tags.map(t => `#${t}`).join(' ');

        const response = await admin.messaging().sendEachForMulticast({
            tokens: fcmTokens,
            notification: {
                title: 'Nouveau post sur vos tags préférés ! 🤩',
                body: `Découvrez "${postTitle}" contenant les tags : ${formattedTags}`,
            },
            data: {
                type: NotificationType.NEW_POST_TAG,
                postId: postId,
                postTitle: postTitle
            },
        });

        await this.cleanupTokens(fcmTokens, response);
    }

    async notifyNewPostLocalisation(userId: number, postId: string, postTitle: string, localisation: string, groupId?: number): Promise<void> {
        const localisationRecord = await this.prisma.localisation.findUnique({
            where: { name: localisation },
            select: { id: true, name: true }
        });
        if (!localisationRecord) return;

        let group = null;
        if (groupId) {
            group = await this.prisma.group.findUnique({
                where: { id: groupId },
                select: { isGroupPrivate: true }
            });
        }

        const followers = await this.prisma.localisationFollow.findMany({
            where: {
                localisationId: localisationRecord.id,
                followerId: { not: userId },
                ...(group?.isGroupPrivate ? {
                    follower: {
                        memberOf: { some: { groupId } }
                    }
                } : {})
            },
            select: { followerId: true }
        });

        const recipientIds = followers.map(f => f.followerId);
        if (recipientIds.length === 0) return;

        await this.prisma.notification.createMany({
            data: recipientIds.map(id => ({
                targetUserId: userId,
                userId: id,
                type: NotificationType.NEW_POST_LOCALISATION,
                targetPostId: postId,
                targetLocalisationId: localisationRecord.id
            }))
        });

        const tokenRows = await this.prisma.fcmToken.findMany({
            where: { userId: { in: recipientIds } },
            select: { fcmToken: true }
        });

        const fcmTokens = [...new Set(tokenRows.map(r => r.fcmToken))];
        if (fcmTokens.length === 0) return;

        const response = await admin.messaging().sendEachForMulticast({
            tokens: fcmTokens,
            notification: {
                title: 'Nouveau post sur une localisation que vous suivez ! 📍',
                body: `Découvrez "${postTitle}" à ${localisationRecord.name}`,
            },
            data: {
                type: NotificationType.NEW_POST_LOCALISATION,
                postId: postId,
                postTitle: postTitle,
                localisationId: localisationRecord.id.toString()
            },
        });

        await this.cleanupTokens(fcmTokens, response);
    }

    async notifyNewPostLike(postId: string, userId?: number): Promise<void> {
        const post = await this.prisma.post.findUniqueOrThrow({ where: { id: postId } });

        await this.prisma.notification.create({ data: {
            targetUserId: userId ? userId : null,
            userId: post.userId,
            type: NotificationType.NEW_POST_LIKE,
            targetPostId: postId
        }});

        const fcmTokens = (await this.prisma.fcmToken.findMany({ where: { userId: post.userId } })).map(t => t.fcmToken);
        if (fcmTokens.length === 0) return;

        const response = await admin.messaging().sendEachForMulticast({
            tokens: fcmTokens,
            notification: {
                title: 'Nouveau like ! ❤️',
                body: `Quelqu'un a aimé votre publication : "${post.title}"`,
            },
            data: {
                type: NotificationType.NEW_POST_LIKE,
                postId: postId,
                postTitle: post.title
            },
        });

        await this.cleanupTokens(fcmTokens, response);
    }



    private async cleanupTokens(tokens: string[], response: admin.messaging.BatchResponse): Promise<void> {
        const invalidTokens: string[] = [];
        response.responses.forEach((res, idx) => {
            if (!res.success && res.error?.code === 'messaging/registration-token-not-registered') {
                invalidTokens.push(tokens[idx]);
            }
        });

        if (invalidTokens.length > 0) {
            await this.prisma.fcmToken.deleteMany({
                where: { fcmToken: { in: invalidTokens } }
            });
        }
    }



    async addUserFollow(follower: UserSession, followingId: number): Promise<ResponseMessage> {
        await this.prisma.userFollow.create({ data: {
            followerId: follower.id,
            followingId
        }});

        return { status: true, message: 'Added new user post notifications' };
    }

    async deleteUserFollow(follower: UserSession, followingId: number): Promise<ResponseMessage> {
        await this.prisma.$transaction([
            this.prisma.notification.deleteMany({ where: {
                userId: follower.id,
                targetUserId: followingId,
                type: NotificationType.NEW_POST_USER
            }}),
            this.prisma.userFollow.delete({ where: {
                followerId_followingId: {
                    followerId: follower.id,
                    followingId
                }
            }})
        ]);

        return { status: true, message: 'Deleted user post notifications' };
    }



    async addGroupFollow(follower: UserSession, groupId: number): Promise<ResponseMessage> {
        await this.prisma.member.findUniqueOrThrow({ where: { groupId_userId: { groupId, userId: follower.id } } });

        await this.prisma.groupFollow.create({ data: {
            followerId: follower.id,
            groupId
        }});

        return { status: true, message: 'Added new group post notifications' };
    }

    async deleteGroupFollow(follower: UserSession, groupId: number): Promise<ResponseMessage> {
        await this.prisma.$transaction([
            this.prisma.notification.deleteMany({ where: {
                userId: follower.id,
                targetGroupId: groupId,
                type: NotificationType.NEW_POST_GROUP
            }}),
            this.prisma.groupFollow.delete({ where: {
                followerId_groupId: {
                    followerId: follower.id,
                    groupId
                }
            }})
        ]);

        return { status: true, message: 'Deleted new group post notifications' };
    }



    async addTagFollow(follower: UserSession, tagId: number): Promise<ResponseMessage> {
        await this.prisma.tagFollow.create({ data: {
            followerId: follower.id,
            tagId
        }});

        return { status: true, message: 'Added new tag post notifications' };
    }

    async deleteTagFollow(follower: UserSession, tagId: number): Promise<ResponseMessage> {
        await this.prisma.$transaction([
            this.prisma.notification.deleteMany({ where: {
                userId: follower.id,
                targetTagId: tagId,
                type: NotificationType.NEW_POST_TAG
            }}),
            this.prisma.tagFollow.delete({ where: {
                followerId_tagId: {
                    followerId: follower.id,
                    tagId
                }
            }})
        ]);

        return { status: true, message: 'Deleted new tag post notifications' };
    }



    async addLocalisationFollow(follower: UserSession, localisationId: number): Promise<ResponseMessage> {
        await this.prisma.localisationFollow.create({ data: {
            followerId: follower.id,
            localisationId
        }});

        return { status: true, message: 'Added new localisation post notifications' };
    }

    async deleteLocalisationFollow(follower: UserSession, localisationId: number): Promise<ResponseMessage> {
        await this.prisma.$transaction([
            this.prisma.notification.deleteMany({ where: {
                userId: follower.id,
                targetLocalisationId: localisationId,
                type: NotificationType.NEW_POST_LOCALISATION
            }}),
            this.prisma.localisationFollow.delete({ where: {
                followerId_localisationId: {
                    followerId: follower.id,
                    localisationId
                }
            }})
        ]);

        return { status: true, message: 'Deleted new localisation post notifications' };
    } 
}
