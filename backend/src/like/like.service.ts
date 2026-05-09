import { Injectable } from '@nestjs/common';
import type { UserSession } from 'src/utils/dto/userSession.dto';
import { PrismaService } from 'src/prisma/prisma.service';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { NotificationService } from 'src/notification/notification.service';
import { NotificationType } from '@prisma/client';

@Injectable()
export class LikeService {
    constructor(
        private readonly prisma: PrismaService,
        private readonly notification: NotificationService
    ) {}



    async addLikePost(postId: string, user: UserSession, anonymousToken: string): Promise<ResponseMessage> {
        if (user) {
            await this.prisma.like.create({ data: {
                postId,
                userId: user.id
            }});
        }

        else if (anonymousToken) {
            await this.prisma.like.create({ data: {
                postId,
                anonymousUserId: anonymousToken
            }});
        }

        this.notification.notifyNewPostLike(postId, user?.id);

        return { status: true, message: 'Post liked' };
    }



    async deleteLikePost(postId: string, user: UserSession, anonymousToken: string): Promise<ResponseMessage> {
        if (user) {
            await this.prisma.$transaction([
                this.prisma.like.deleteMany({ where: {
                    postId,
                    userId: user.id
                }}),
                this.prisma.notification.deleteMany({ where: {
                    targetPostId: postId,
                    targetUserId: user.id,
                    type: NotificationType.NEW_POST_LIKE
                }})
            ]);
        }

        else if (anonymousToken) {
            await this.prisma.like.deleteMany({ where: {
                postId,
                anonymousUserId: anonymousToken
            }});
        }

        return { status: true, message: 'Post like deleted' };
    }



    async addLikeComment(commentId: number, user: UserSession, anonymousToken: string): Promise<ResponseMessage> {
        if (user) {
            await this.prisma.commentLike.create({ data: {
                commentId,
                userId: user.id
            }});
        }

        else if (anonymousToken) {
            await this.prisma.commentLike.create({ data: {
                commentId,
                anonymousUserId: anonymousToken
            }});
        }

        return { status: true, message: 'Comment liked' };
    }



    async deleteLikeComment(commentId: number, user: UserSession, anonymousToken: string): Promise<ResponseMessage> {
        if (user) {
            await this.prisma.commentLike.deleteMany({ where: {
                commentId,
                userId: user.id
            }});
        }

        else if (anonymousToken) {
            await this.prisma.commentLike.deleteMany({ where: {
                commentId,
                anonymousUserId: anonymousToken
            }});
        }

        return { status: true, message: 'Comment like deleted' };
    }
}
