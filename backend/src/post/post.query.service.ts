import { Injectable } from "@nestjs/common";
import { CdnService } from "src/cdn/cdn.service";
import { PrismaService } from "src/prisma/prisma.service";
import { PostsInfos, PostDto } from "./dto/postInfos.dto";
import { CommentInfos } from "./dto/comment.dto";
import { PostType } from "@prisma/client";
import { LocalisationUtil } from "src/utils/localisation/localisation.util";
import { getNextCursor } from "src/utils/paginator/paginate.util";

@Injectable()
export class PostQueryService {
    constructor(
        private readonly prisma: PrismaService,
        private readonly cdn: CdnService,
        private readonly locUtil: LocalisationUtil
    ) {}



    async getPosts(limit: number, cursor?: string, userId?: number, anonymousToken?: string, q?: string, tag?: string[], type?: PostType, loc?: string, dist?: number): Promise<PostsInfos> {
        const realUser = userId ? { userId } : anonymousToken ? { anonymousUserId: anonymousToken } : null;

        const cleanLocalisation: string | null = loc ? loc.toLowerCase().trim() : null;
        let localisationIds: number[] | null = null;
        if (cleanLocalisation && dist) {
            localisationIds = await this.getNearbyLocalisationIds(cleanLocalisation, dist);
            if (!localisationIds || localisationIds.length === 0) return { posts: [], nextCursor: undefined };
        }

        const tags = tag ? (Array.isArray(tag) ? tag : [tag]) : undefined;

        const posts = await this.prisma.post.findMany({
            take: limit + 1,
            skip: cursor ? 1 : 0,
            ...(cursor ? { cursor: { id: cursor } } : {}),
            where: {
                AND: [
                    q ? {
                        OR: [
                            { title: { contains: q, mode: 'insensitive' } },
                            { description: { contains: q, mode: 'insensitive' } },
                            { User: { username: { contains: q, mode: 'insensitive' } } },
                            { Group: { name: { contains: q, mode: 'insensitive' } } }
                        ]
                    } : {},
                    tags && tags.length > 0 ? {
                        AND: tags.map(tagName => ({ 
                            postTags: { some: { tag: { name: tagName } } } 
                        }))
                    } : {},
                    localisationIds ? { localisationId: { in: localisationIds } } : cleanLocalisation ? { Localisation: { name: cleanLocalisation } } : {},
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
            orderBy: [{ creationDate: 'desc' }, { id: 'desc' }],
            include: {
                Localisation: { select: { id: true, name: true, long: true, lat: true } },
                Group: { select: { id: true, name: true, avatar: true, members: userId ? { where: { userId }, select: { userId: true } } : false } },
                User: { select: { id: true, username: true, avatar: true } },
                postTags: { select: { tag: { select: { name: true } } } },
                likes: realUser ? { where: realUser, select: { id: true } } : false,
                bookmarks: userId ? { where: { userId }, select: { userId: true } } : false
            }
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
                isBookmarked: userId ? post.bookmarks?.length > 0 : false
            })),
            nextCursor: getNextCursor(posts, limit)
        };
    }



    async getPost(postId: string, userId?: number, anonymousToken?: string): Promise<PostDto> {
        const realUser = userId ? { userId } : anonymousToken ? { anonymousUserId: anonymousToken } : null;

        const post = await this.prisma.post.findUniqueOrThrow({
            where: { id: postId },
            include: {
                Localisation: { select: { id: true, name: true, long: true, lat: true } },
                Group: { select: { id: true, name: true, avatar: true, members: userId ? { where: { userId }, select: { userId: true } } : false } },
                User: { select: { id: true, username: true, avatar: true } },
                postTags: { select: { tag: { select: { name: true } } } },
                likes: realUser ? { where: realUser, select: { id: true } } : false,
                bookmarks: realUser ? { where: realUser, select: { userId: true } } : false
            }
        });

        return {
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
            isBookmarked: post.bookmarks?.length > 0,
        };
    }



    async getPostComments(postId: string, userId?: number, anonymousToken?: string): Promise<CommentInfos[]> {
        const realUser = userId ? { userId } : anonymousToken ? { anonymousUserId: anonymousToken } : null;

        const comments = await this.prisma.comment.findMany({
            where: { postId, parentId: null },
            select: {
                id: true,
                content: true,
                creationDate: true,
                isEdited: true,
                updatedAt: true,
                nbLikes: true,
                nbReplies: true,
                User: { select: { id: true, username: true, avatar: true } },
                commentLikes: realUser ? { where: realUser, select: { id: true } } : false,
            },
            orderBy: [{ creationDate: 'desc'}, { id: 'desc' }]
        });

        return comments.map(c => ({
            id: c.id,
            content: c.content,
            creationDate: c.creationDate,
            isEdited: c.isEdited,
            updatedAt: c.updatedAt ?? undefined,
            nbLikes: c.nbLikes,
            nbReplies: c.nbReplies,
            userId: c.User.id,
            username: c.User.username,
            avatar: this.cdn.getAvatarUrl(c.User.avatar),
            isLiked: c.commentLikes?.length > 0
        }));
    }



    async getPostCommentsReplies(postId: string, commentId: number, userId?: number, anonymousToken?: string): Promise<CommentInfos[]> {
        const realUser = userId ? { userId } : anonymousToken ? { anonymousUserId: anonymousToken } : null;

        const comments = await this.prisma.comment.findMany({
            where: { postId, parentId: commentId },
            select: {
                id: true,
                content: true,
                creationDate: true,
                isEdited: true,
                updatedAt: true,
                nbLikes: true,
                nbReplies: true,
                User: { select: { id: true, username: true, avatar: true } },
                commentLikes: realUser ? { where: realUser, select: { id: true } } : false,
            },
            orderBy: [{ creationDate: 'desc' }, { id: 'desc' }]
        });

        return comments.map(c => ({
            id: c.id,
            content: c.content,
            creationDate: c.creationDate,
            isEdited: c.isEdited,
            updatedAt: c.updatedAt ?? undefined,
            nbLikes: c.nbLikes,
            nbReplies: c.nbReplies,
            userId: c.User.id,
            username: c.User.username,
            avatar: this.cdn.getAvatarUrl(c.User.avatar),
            isLiked: c.commentLikes?.length > 0,
            isYours: userId ? userId === c.User.id : false
        }));
    }



    private async getNearbyLocalisationIds(locName: string, dist: number): Promise<number[] | null> {
        let lat: number, lng: number;

        const refLoc = await this.prisma.localisation.findFirst({
            where: { name: locName },
            select: { lat: true, long: true }
        });

        if (refLoc) {
            lat = Number(refLoc.lat);
            lng = Number(refLoc.long);
        } else {
            const coords = await this.locUtil.getCoordinates(locName);
            lat = coords.lat;
            lng = coords.long;
        }

        const latDelta = dist / 111;
        const lngDelta = dist / (111 * Math.cos((lat * Math.PI) / 180));

        const nearby = await this.prisma.$queryRaw<{ id: number }[]>`
            SELECT id FROM "Localisation"
            WHERE lat BETWEEN ${lat - latDelta} AND ${lat + latDelta}
                AND long BETWEEN ${lng - lngDelta} AND ${lng + lngDelta}
                AND (6371 * acos(
                    cos(radians(${lat})) * cos(radians(lat)) *
                    cos(radians(long) - radians(${lng})) +
                    sin(radians(${lat})) * sin(radians(lat))
                )) <= ${dist}
            `;

        return nearby.map(r => r.id);
    }
}