import { Injectable, OnModuleInit } from '@nestjs/common';
import * as admin from 'firebase-admin';
import { join } from 'path';
import { PrismaService } from 'src/prisma/prisma.service';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { NotificationList } from './dto/notificationList.dto';
import { CdnService } from 'src/cdn/cdn.service';
import { UserSession } from 'src/utils/dto/userSession.dto';
import { NotificationType } from '@prisma/client';

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



    async getNotifications(limit: number, user: UserSession, cursor?: string): Promise<NotificationList[]> {
        const take = Math.min(Math.max(limit, 1), 50);
        const skip = cursor ? 1 : 0;
        const cursorObj = cursor ? { id: parseInt(cursor) } : undefined;

        const notifications = await this.prisma.notification.findMany({
            take,
            skip,
            cursor: cursorObj,
            where: { userId: user.id },
            orderBy: [{ creationDate: 'desc' }, { id: 'desc' }],
            include: {
                targetUser: { select: { id: true, username: true, avatar: true } },
                post: { select: { imageExt: true  } },
                group: { select: { id: true, name: true, avatar: true } },
                tag: { select: { id: true, name: true } }
            }
        });

        return notifications.map(n => ({
            id: n.id,
            type: n.type,
            creationDate: n.creationDate,
            isRead: n.isRead,

            postId: n.targetPostId ?? undefined,
            postImage: (n.targetPostId && n.post?.imageExt) ? this.cdn.getPostUrl(n.targetPostId, n.post.imageExt) : undefined,
            postUserId: n.targetUser.id,
            postUsername: n.targetUser.username,
            postUserAvatar: n.targetUser.avatar ? this.cdn.getAvatarUrl(n.targetUser.avatar) : undefined,

            groupId: n.targetGroupId ?? undefined,
            groupName: n.group?.name,
            groupAvatar: n.group?.avatar ? this.cdn.getGroupAvatarUrl(n.group.avatar) : undefined,

            tagId: n.tag?.id,
            tagName: n.tag?.name
        }));
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
                title: 'notification_new_post_title',
                body: 'notification_new_post_body',
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
                title: 'notification_new_post_group_title',
                body: 'notification_new_post_group_body',
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

        const response = await admin.messaging().sendEachForMulticast({
            tokens: fcmTokens,
            notification: {
                title: 'notification_new_post_tag_title',
                body: 'notification_new_post_tag_body',
            },
            data: {
                type: NotificationType.NEW_POST_TAG,
                postId: postId,
                postTitle: postTitle
            },
        });

        await this.cleanupTokens(fcmTokens, response);
    }



    private async cleanupTokens(tokens: string[], response: admin.messaging.BatchResponse) {
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
}
