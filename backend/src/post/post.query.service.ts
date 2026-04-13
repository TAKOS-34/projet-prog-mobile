import { Injectable } from "@nestjs/common";
import { CdnService } from "src/cdn/cdn.service";
import { PrismaService } from "src/prisma/prisma.service";
import { PostInfos } from "./dto/postInfos.dto";
import { CommentInfos } from "./dto/comment.dto";

@Injectable()
export class PostQueryService {
    constructor(
        private readonly prisma: PrismaService,
        private readonly cdn: CdnService
    ) {}



    async getFeed(limit: number, cursor?: string, userId?: number, anonymousToken?: string): Promise<PostInfos[]> {
        const realUser = userId ? { userId } : anonymousToken ? { anonymousUserId: anonymousToken } : null;

        const posts = await this.prisma.post.findMany({
            take: limit,
            skip: cursor ? 1 : 0,
            ...(cursor ? { cursor: { id: cursor } } : {}),
            where: {
                OR: [
                    { Group: null },
                    { Group: { isGroupPrivate: false } },
                    userId ? {
                        Group: {
                            members: { some: { userId } }
                        }
                    } : {}
                ]
            },
            orderBy: { creationDate: 'desc', id: 'desc' },
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



    async getPost(postId: string, userId?: number, anonymousToken?: string): Promise<PostInfos> {
        const realUser = userId ? { userId } : anonymousToken ? { anonymousUserId: anonymousToken } : null;

        const post = await this.prisma.post.findUniqueOrThrow({
            where: { id: postId },
            include: {
                Group: { select: { id: true, name: true, avatar: true } },
                User: { select: { id: true, username: true, avatar: true } },
                postTags: { select: { tag: { select: { name: true } } } },
                likes: realUser ? { where: realUser, select: { id: true } } : false
            }
        });

        return {
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
            orderBy: { creationDate: "asc" }
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
            orderBy: { creationDate: "asc" }
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
}