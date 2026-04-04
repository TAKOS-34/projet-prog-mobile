import { Injectable } from '@nestjs/common';
import type { User } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';

@Injectable()
export class LikeService {
    constructor(
        private readonly prisma: PrismaService
    ) {}



    async addLikePost(postId: string, user: User, anonymousToken: string): Promise<ResponseMessage> {
        if (user) {
            await this.prisma.like.create({ data: {
                postId,
                userId: user.id
            }});
        }

        if (anonymousToken) {
            await this.prisma.like.create({ data: {
                postId,
                anonymousUserId: anonymousToken
            }});
        }

        return { status: true, message: 'Post liked' };
    }



    async deleteLikePost(postId: string, user: User, anonymousToken: string): Promise<ResponseMessage> {
        if (user) {
            await this.prisma.like.deleteMany({ where: {
                postId,
                userId: user.id
            }});
        }

        if (anonymousToken) {
            await this.prisma.like.deleteMany({ where: {
                postId,
                anonymousUserId: anonymousToken
            }});
        }

        return { status: true, message: 'Post like deleted' };
    }



    async addLikeComment(commentId: number, user: User, anonymousToken: string): Promise<ResponseMessage> {
        if (user) {
            await this.prisma.commentLike.create({ data: {
                commentId,
                userId: user.id
            }});
        }

        if (anonymousToken) {
            await this.prisma.commentLike.create({ data: {
                commentId,
                anonymousUserId: anonymousToken
            }});
        }

        return { status: true, message: 'Comment liked' };
    }



    async deleteLikeComment(commentId: number, user: User, anonymousToken: string): Promise<ResponseMessage> {
        if (user) {
            await this.prisma.commentLike.deleteMany({ where: {
                commentId,
                userId: user.id
            }});
        }

        if (anonymousToken) {
            await this.prisma.commentLike.deleteMany({ where: {
                commentId,
                anonymousUserId: anonymousToken
            }});
        }

        return { status: true, message: 'Comment like deleted' };
    }
}
