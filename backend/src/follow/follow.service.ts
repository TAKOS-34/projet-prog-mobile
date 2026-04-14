import { Injectable } from '@nestjs/common';
import { PrismaService } from 'src/prisma/prisma.service';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { UserSession } from 'src/utils/dto/userSession.dto';
import { FollowedUserList } from './dto/followedUserList.dto';
import { FollowedGroupList } from './dto/followedGroupList.dto';
import { FollowedTagList } from './dto/followedTagList.dto';
import { CdnService } from 'src/cdn/cdn.service';

@Injectable()
export class FollowService {
    constructor(
        private readonly prisma: PrismaService,
        private readonly cdn: CdnService
    ) {}



    async getFollowedUser(user: UserSession): Promise<FollowedUserList[]> {
        const users = await this.prisma.userFollow.findMany({
            where: { followerId: user.id },
            select: {
                following: {
                    select: {
                        id: true,
                        username: true,
                        avatar: true
                    }
                }
            }
        });

        return users.map(u => ({
            id: u.following.id,
            username: u.following.username,
            avatar: this.cdn.getAvatarUrl(u.following.avatar)
        }));
    }



    async addFollowUser(userId: number, user: UserSession): Promise<ResponseMessage> {
        await this.prisma.userFollow.create({ data: {
            followerId: user.id,
            followingId: userId
        }});

        return { status: true, message: 'User follow added' };
    }



    async deleteFollowUser(userId: number, user: UserSession): Promise<ResponseMessage> {
        await this.prisma.userFollow.delete({ where: {
            followerId_followingId: {
                followerId: user.id,
                followingId: userId
            }
        }});

        return { status: true, message: 'User follow deleted' };
    }



    async getFollowedGroup(user: UserSession): Promise<FollowedGroupList[]> {
        const groups = await this.prisma.groupFollow.findMany({
            where: { followerId: user.id },
            select: {
                following: {
                    select: {
                        id: true,
                        name: true,
                        avatar: true
                    }
                }
            }
        });

        return groups.map(g => ({
            id: g.following.id,
            name: g.following.name,
            avatar: this.cdn.getGroupAvatarUrl(g.following.avatar)
        }));
    }



    async addFollowGroup(groupId: number, user: UserSession): Promise<ResponseMessage> {
        await this.prisma.groupFollow.create({ data: {
            followerId: user.id,
            groupId: groupId
        }});

        return { status: true, message: 'Group follow added' };
    }



    async deleteFollowGroup(groupId: number, user: UserSession): Promise<ResponseMessage> {
        await this.prisma.groupFollow.delete({ where: {
            followerId_groupId: {
                followerId: user.id,
                groupId: groupId
            }
        }});

        return { status: true, message: 'Group follow deleted' };
    }



    async getFollowedTag(user: UserSession): Promise<FollowedTagList[]> {
        const tags = await this.prisma.tagFollow.findMany({
            where: { followerId: user.id },
            select: {
                following: {
                    select: {
                        id: true,
                        name: true
                    }
                }
            }
        });

        return tags.map(t => ({
            id: t.following.id,
            name: t.following.name
        }));
    }



    async addFollowTag(tagId: number, user: UserSession): Promise<ResponseMessage> {
        await this.prisma.tagFollow.create({ data: {
            followerId: user.id,
            tagId: tagId
        }});

        return { status: true, message: 'Tag follow added' };
    }



    async deleteFollowTag(tagId: number, user: UserSession): Promise<ResponseMessage> {
        await this.prisma.tagFollow.delete({ where: {
            followerId_tagId: {
                followerId: user.id,
                tagId: tagId
            }
        }});

        return { status: true, message: 'Tag follow deleted' };
    }
}
