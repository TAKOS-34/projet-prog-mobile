import { Injectable, OnModuleInit } from '@nestjs/common';
import * as admin from 'firebase-admin';
import { join } from 'path';
import { PrismaService } from 'src/prisma/prisma.service';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { NotificationList } from './dto/notificationList.dto';
import { CdnService } from 'src/cdn/cdn.service';
import { UserSession } from 'src/utils/dto/userSession.dto';

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
            orderBy: [
                { creationDate: 'desc' },
                { id: 'desc' }
            ],
            include: {
                sender: { select: { username: true, avatar: true } },
                post: { select: { imageExt: true } },
                group: { select: { name: true, avatar: true } },
                tag: { select: { name: true } }
            }
        });

        return notifications.map(n => ({
            id: n.id,
            type: n.type,
            creationDate: n.creationDate,
            isRead: n.isRead,
            
            senderId: n.senderId ?? undefined,
            senderName: n.sender?.username,
            senderAvatar: n.sender?.avatar ? this.cdn.getAvatarUrl(n.sender.avatar) : undefined,

            postId: n.targetPostId ?? undefined,
            postImage: (n.targetPostId && n.post?.imageExt) 
                ? this.cdn.getPostUrl(n.targetPostId, n.post.imageExt) 
                : undefined,

            groupId: n.targetGroupId ?? undefined,
            groupName: n.group?.name,
            groupAvatar: n.group?.avatar ? this.cdn.getGroupAvatarUrl(n.group.avatar) : undefined,

            tagName: n.tag?.name
        }));
    }
}
