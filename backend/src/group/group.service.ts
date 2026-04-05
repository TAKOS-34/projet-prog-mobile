import { BadRequestException, Injectable, UnauthorizedException } from '@nestjs/common';
import { UserSession } from 'src/utils/dto/userSession.dto';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { CreateGroupDto } from './dto/createGroup.dto';
import { CdnService } from 'src/cdn/cdn.service';
import { PrismaService } from 'src/prisma/prisma.service';
import { randomUUID } from 'crypto';
import sharp from 'sharp';
import { UserList } from 'src/group/dto/userList.dto';
import { GroupInfos } from './dto/groupInfos.dto';
import { PostInfos } from 'src/post/dto/postInfos.dto';

@Injectable()
export class GroupService {
    constructor(
        private readonly prisma: PrismaService,
        private readonly cdn: CdnService
    ) {}



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
                admin: true,
                nbMembers: true,
                nbPosts: true,
                members: user ? { where: { userId: user.id }, select: { userId: true } } : false
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
            isAdmin: user ? group.admin === user.id : false
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

    async getGroupPosts(groupId: number, userId?: number, anonymousToken?: string): Promise<PostInfos[]> {
        const realUser = userId ? { userId } : anonymousToken ? { anonymousUserId: anonymousToken } : null;

        const posts = await this.prisma.post.findMany({
            where: { groupId },
            include: {
                Group: { select: { id: true, name: true, avatar: true } },
                User: { select: { id: true, username: true, avatar: true } },
                postTags: { select: { tag: { select: { name: true } } } },
                likes: realUser ? { where: realUser, select: { id: true } } : false
            }
        });

        return posts.map(post => ({
            id: post.id,
            image: this.cdn.getPostUrl(post.id, post.imageExt),
            creationDate: post.creationDate,
            isEdited: post.isEdited,
            updatedAt: post.updatedAt ?? undefined,
            title: post.title,
            localisation: post.localisation,
            long: post.long,
            lat: post.lat,
            description: post.description ?? undefined,
            audio: post.audio ? this.cdn.getAudioUrl(post.audio) : undefined,
            nbLikes: post.nbLikes,
            nbComments: post.nbComments,
            userId: post.User.id,
            username: post.User.username,
            avatar: this.cdn.getAvatarUrl(post.User.avatar),
            groupId: post.Group?.id ?? undefined,
            groupName: post.Group?.name ?? undefined,
            groupAvatar: post.Group?.avatar ? this.cdn.getGroupAvatarUrl(post.Group.avatar) : undefined,
            tags: post.postTags.map(pt => pt.tag.name),
            isLiked: post.likes?.length > 0
        }));
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
                admin: user.id
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
            throw new BadRequestException('Error during group creation');
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
                admin: user.id
            },
            select: { id: true }
        });

        if (isUserAdmin) {
            throw new BadRequestException('You cannot quit the group as group admin');
        }

        await this.prisma.member.delete({
            where: {
                groupId_userId: {
                    userId: user.id,
                    groupId
                }
            }
        });

        return { status: true, message: 'Group quited' };
    }
}
