import { Injectable } from '@nestjs/common';
import { PrismaService } from 'src/prisma/prisma.service';
import { GroupSearch } from './dto/groupSearch.dto';
import { CdnService } from 'src/cdn/cdn.service';
import { PostSearch } from './dto/postSearch.dto';

@Injectable()
export class SearchService {
    constructor(
        private readonly prisma: PrismaService,
        private readonly cdn: CdnService
    ) {}



    async searchGroups(name: string): Promise<GroupSearch[]> {
        const groups = await this.prisma.group.findMany({
            where: { name: { contains: name } },
            select: { id: true, name: true, avatar: true, creationDate: true, isGroupPrivate: true, nbMembers: true, nbPosts: true },
            orderBy: { nbMembers: 'desc', creationDate: 'desc' },
            take: 20
        });

        return groups.map(g => ({
            id: g.id,
            name: g.name,
            avatar: this.cdn.getGroupAvatarUrl(g.avatar),
            creationDate: g.creationDate,
            isGroupPrivate: g.isGroupPrivate,
            nbMembers: g.nbMembers,
            nbPosts: g.nbPosts
        }));
    }



    async searchPosts(q?: string, tag?: string, cursor?: string, userId?: number, anonymousToken?: string): Promise<PostSearch[]> {
        const realUser = userId ? { userId } : anonymousToken ? { anonymousUserId: anonymousToken } : null;

        const posts = await this.prisma.post.findMany({
            where: {
                AND: [
                    q ? {
                        OR: [
                            { title: { contains: q, mode: 'insensitive' } },
                            { description: { contains: q, mode: 'insensitive' } }
                        ]
                    } : {},
                    tag ? {
                        postTags: { some: { tag: { name: tag } } }
                    } : {},
                    {
                    OR: [
                        { Group: null },
                        { Group: { isGroupPrivate: false } },
                        userId ? {
                            Group: {
                                members: { some: { userId } }
                            }
                        } : {}
                    ]
                }
                ]
            },
            take: 20,
            skip: cursor ? 1 : 0,
            ...(cursor ? { cursor: { id: cursor } } : {}),
            orderBy: { creationDate: 'desc', id: 'desc' },
            select: {
                id: true,
                imageExt: true,
                creationDate: true,
                title: true,
                nbLikes: true,
                nbComments: true,
                User: {
                    select: { id: true, username: true, avatar: true }
                },
                Group: {
                    select: { id: true, name: true, avatar: true }
                },
                likes: realUser ? { where: realUser, select: { id: true } } : false
            }
        });

        return posts.map(p => ({
            id: p.id,
            image: this.cdn.getPostUrl(p.id, p.imageExt),
            creationDate: p.creationDate,
            title: p.title,
            nbLikes: p.nbLikes,
            nbComments: p.nbComments,
            userId: p.User.id,
            username: p.User.username,
            avatar: this.cdn.getAvatarUrl(p.User.avatar),
            isLiked: p.likes?.length > 0,
            groupId: p.Group?.id ?? undefined,
            groupName: p.Group?.name ?? undefined,
            groupAvatar: this.cdn.getGroupAvatarUrl(p.Group?.avatar ?? null) ?? undefined
        }));
    }
}
