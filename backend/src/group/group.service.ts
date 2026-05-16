import { BadRequestException, Injectable, InternalServerErrorException, UnauthorizedException } from '@nestjs/common';
import { UserSession } from 'src/utils/dto/userSession.dto';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { CreateGroupDto } from './dto/createGroup.dto';
import { CdnService } from 'src/cdn/cdn.service';
import { PrismaService } from 'src/prisma/prisma.service';
import { randomUUID } from 'crypto';
import sharp from 'sharp';
import { UserList } from 'src/group/dto/userList.dto';
import { GroupInfos } from './dto/groupInfos.dto';
import { PostsInfos } from 'src/post/dto/postInfos.dto';
import { getNextCursor } from 'src/utils/paginator/paginate.util';
import { GroupSearch } from 'src/search/dto/groupSearch.dto';

@Injectable()
export class GroupService {
    constructor(
        private readonly prisma: PrismaService,
        private readonly cdn: CdnService
    ) {}



    async getMyGroups(user: UserSession): Promise<GroupInfos[]> {
        const groups = await this.prisma.group.findMany({
            where: { members: { some: { userId: user.id } } },
            select: {
                id: true,
                name: true,
                avatar: true,
                description: true,
                creationDate: true,
                isGroupPrivate: true,
                adminId: true,
                nbMembers: true,
                nbPosts: true,
                members: { where: { userId: user.id }, select: { userId: true } },
                followers: { where: { followerId: user.id }, select: { followerId: true }, take: 1 },
            }
        });

        return groups.map(group => ({
            id: group.id,
            name: group.name,
            avatar: this.cdn.getGroupAvatarUrl(group.avatar),
            description: group.description ?? undefined,
            creationDate: group.creationDate,
            isGroupPrivate: group.isGroupPrivate,
            nbMembers: group.nbMembers,
            nbPosts: group.nbPosts,
            isMember: user ? group.members.length > 0 : false,
            isAdmin: group.adminId === user.id,
            isFollowing: group.followers.length > 0
        }));
    }



    async getPopularGroups(userId?: number): Promise<GroupSearch[]> {
        const groups = await this.prisma.group.findMany({
            select: {
                id: true,
                name: true,
                avatar: true,
                creationDate: true,
                isGroupPrivate: true,
                nbMembers: true,
                nbPosts: true,
                ...(userId ? { members: { where: { userId }, select: { userId: true }, take: 1 } } : {})
            },
            orderBy: [{ nbMembers: 'desc' }, { creationDate: 'desc' }],
            take: 20
        });

        return groups.map(g => ({
            id: g.id,
            name: g.name,
            avatar: this.cdn.getGroupAvatarUrl(g.avatar),
            creationDate: g.creationDate,
            isGroupPrivate: g.isGroupPrivate,
            isMember: userId ? g.members.length > 0 : false,
            nbMembers: g.nbMembers,
            nbPosts: g.nbPosts
        }));
    }



    async getGroupInfos(groupId: number, user?: UserSession): Promise<GroupInfos> {
        const group = await this.prisma.group.findUniqueOrThrow({
            where: { id: groupId },
            select: { 
                id: true,
                name: true,
                avatar: true,
                description: true,
                creationDate: true,
                isGroupPrivate: true,
                adminId: true,
                nbMembers: true,
                nbPosts: true,
                members: user ? { where: { userId: user.id }, select: { userId: true } } : false,
                followers: user ? { where: { followerId: user.id }, select: { followerId: true }, take: 1 } : false
            }});

        return {
            id: group.id,
            name: group.name,
            avatar: this.cdn.getGroupAvatarUrl(group.avatar),
            description: group.description ?? undefined,
            creationDate: group.creationDate,
            isGroupPrivate: group.isGroupPrivate,
            nbMembers: group.nbMembers,
            nbPosts: group.nbPosts,
            isMember: user ? group.members.length > 0 : false,
            isAdmin: user ? group.adminId === user.id : false,
            isFollowing: user ? group.followers.length > 0 : false
        }
    }

    async getGroupMembers(groupId: number): Promise<UserList[]> {
        const group = await this.prisma.group.findUniqueOrThrow({
            where: { id: groupId },
            select: { members: { select: { User: { select: {
                id: true,
                username: true,
                avatar: true
            }}}}}
        });

        return group.members.map(member => ({
            id: member.User.id,
            username: member.User.username,
            avatar: this.cdn.getAvatarUrl(member.User.avatar)
        }));
    }

    async getGroupPosts(groupId: number, limit: number, userId?: number, anonymousToken?: string, cursor?: string): Promise<PostsInfos> {
        const realUser = userId ? { userId } : anonymousToken ? { anonymousUserId: anonymousToken } : null;

        const posts = await this.prisma.post.findMany({
            take: limit + 1,
            skip: cursor ? 2 : 0,
            ...(cursor ? { cursor: { id: cursor } } : {}),
            where: { groupId },
            include: {
                Localisation: { select: { id: true, name: true, long: true, lat: true } },
                Group: { select: { id: true, name: true, avatar: true, members: userId ? { where: { userId }, select: { userId: true } } : false } },
                User: { select: { id: true, username: true, avatar: true } },
                postTags: { select: { tag: { select: { name: true } } } },
                likes: realUser ? { where: realUser, select: { id: true } } : false,
                bookmarks: realUser ? { where: realUser, select: { userId: true } } : false
            },
            orderBy: [{ creationDate: 'desc' }, { id: 'desc' }]
        });

        return {
            posts: posts.map(post => ({
                id: post.id,
                image: this.cdn.getPostUrl(post.id, post.imageExt),
                creationDate: post.creationDate,
                isEdited: post.isEdited,
                updatedAt: post.updatedAt ?? undefined,
                title: post.title,
                type: post.type,
                localisation: post.Localisation.name,
                long: Number(post.Localisation.long),
                lat: Number(post.Localisation.lat),
                description: post.description ?? undefined,
                audio: post.audio ? this.cdn.getAudioUrl(post.audio) : undefined,
                audioDuration: post.audioDuration ?? undefined,
                minPrice: post.minPrice ?? undefined,
                maxPrice: post.maxPrice ?? undefined,
                minDuration: post.minDuration ?? undefined,
                maxDuration: post.maxDuration ?? undefined,
                nbLikes: post.nbLikes,
                nbComments: post.nbComments,
                userId: post.User.id,
                username: post.User.username,
                avatar: this.cdn.getAvatarUrl(post.User.avatar),
                groupId: post.Group?.id ?? undefined,
                groupName: post.Group?.name ?? undefined,
                groupAvatar: post.Group?.avatar ? this.cdn.getGroupAvatarUrl(post.Group.avatar) : undefined,
                isMember: userId && post.Group ? post.Group.members?.length > 0 : undefined,
                tags: post.postTags.map(pt => pt.tag.name),
                isLiked: post.likes?.length > 0,
                isYours: userId ? userId === post.User.id : false,
                isBookmarked: post.bookmarks?.length > 0
            })),
            nextCursor: getNextCursor(posts, limit)
        };
    }



    async createGroup(group: CreateGroupDto, user: UserSession, avatar?: Express.Multer.File): Promise<ResponseMessage> {
        const existingGroup = await this.prisma.group.findUnique({
            where: { name: group.name },
            select: { id: true }
        });

        if (existingGroup) {
            throw new BadRequestException('A group with this name already exists');
        }

        const avatarId: string | null = avatar ? randomUUID() + '.jpg' : null;
        const avatarPath: string | null = avatarId ? this.cdn.getGroupAvatarPath(avatarId) : null;

        try {
            const newGroup = await this.prisma.group.create({
                data: {
                name: group.name,
                description: group.description ?? null,
                avatar: avatarId,
                isGroupPrivate: group.isGroupPrivate,
                adminId: user.id
                },
                select: { id: true }
            });

            await this.prisma.member.create({ data: {
                groupId: newGroup.id,
                userId: user.id
            }});

            if (avatar && avatarId && avatarPath) {
                await sharp(avatar.buffer)
                    .resize(500)
                    .jpeg()
                    .toFile(avatarPath);
            }
        } catch (error) {
            throw new InternalServerErrorException('Error during group creation');
        }

        return { status: true, message: 'Group created' };
    }



    async requestToJoin(groupId: number, user: UserSession): Promise<ResponseMessage> {
        const group = await this.prisma.group.findUnique({
            where: { id: groupId },
            select: {
                isGroupPrivate: true,
                members: {
                    select: { userId: true }
                }
            }
        });

        if (!group) {
            throw new BadRequestException('This group does not exist');
        }

        if (group.members?.some(member => member.userId === user.id)) {
            throw new BadRequestException('You are already a member of this group');
        }

        const isUserBanned = await this.prisma.ban.findFirst({
            where: {
                groupId,
                userId: user.id
            }
        });

        if (isUserBanned) {
            throw new UnauthorizedException('Your banned from this group');
        }

        if (group.isGroupPrivate) {
            await this.prisma.requestToJoin.create({ data: {
                groupId,
                userId: user.id
            }});

            return { status: true, message: 'A request to join the group has been sent' };
        } else {
            await this.prisma.member.create({ data : {
                groupId,
                userId: user.id
            }});

            return { status: true, message: 'Group join' };
        }
    }



    async quit(groupId: number, user: UserSession): Promise<ResponseMessage> {
        const isUserAdmin = await this.prisma.group.findFirst({
            where: {
                id: groupId,
                adminId: user.id
            },
            select: { id: true }
        });

        if (isUserAdmin) {
            throw new BadRequestException('You cannot quit the group as group admin');
        }

        await this.prisma.$transaction([
            this.prisma.member.delete({
                where: {
                    groupId_userId: {
                        userId: user.id,
                        groupId
                    }
                }
            }),

            this.prisma.groupFollow.deleteMany({
                where: {
                    followerId: user.id,
                    groupId
                }
            }),

            this.prisma.postBookmark.deleteMany({
                where: {
                    userId: user.id,
                    post: { groupId }
                }
            })
        ]);

        return { status: true, message: 'Group quited' };
    }
}
