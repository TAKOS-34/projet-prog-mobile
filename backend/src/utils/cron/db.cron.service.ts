import { Injectable } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { TripStatus } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';

@Injectable()
export class DbCronService {
    constructor(private readonly prisma: PrismaService) {}

    @Cron(CronExpression.EVERY_HOUR)
    async removeExpiredToken() {
        await this.prisma.userToken.deleteMany({
            where: {
                expirationDate: { lte: new Date() }
            }
        });
    }



    @Cron(CronExpression.EVERY_HOUR)
    async removeInvalidNotifications() {
        const notifications = await this.prisma.notification.findMany({
            where: {
                OR: [
                    { targetGroupId: { not: null } },
                    { post: { groupId: { not: null } } }
                ]
            },
            select: {
                id: true,
                userId: true,
                targetGroupId: true,
                post: {
                    select: {
                        groupId: true
                    }
                }
            }
        });

        if (notifications.length === 0) {
            return;
        }

        const userIds = [...new Set(notifications.map(notification => notification.userId))];
        const groupIds = [...new Set(notifications.flatMap(notification => [notification.targetGroupId, notification.post?.groupId].filter((value): value is number => typeof value === 'number')))];

        if (groupIds.length === 0) {
            return;
        }

        const memberships = await this.prisma.member.findMany({
            where: {
                userId: { in: userIds },
                groupId: { in: groupIds }
            },
            select: {
                userId: true,
                groupId: true
            }
        });

        const membershipSet = new Set(memberships.map(member => `${member.userId}:${member.groupId}`));
        const invalidNotificationIds = notifications
            .filter(notification => {
                if (notification.targetGroupId !== null && !membershipSet.has(`${notification.userId}:${notification.targetGroupId}`)) {
                    return true;
                }

                if (notification.post?.groupId !== null && !membershipSet.has(`${notification.userId}:${notification.post.groupId}`)) {
                    return true;
                }

                return false;
            })
            .map(notification => notification.id);

        if (invalidNotificationIds.length === 0) {
            return;
        }

        await this.prisma.notification.deleteMany({
            where: {
                id: { in: invalidNotificationIds }
            }
        });
    }



    @Cron(CronExpression.EVERY_1ST_DAY_OF_MONTH_AT_MIDNIGHT)
    async removeExpiredUser() {
        const expirationLimit = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);

        await this.prisma.user.deleteMany({
            where: {
                creationDate: { lte: expirationLimit },
                isEmailVerified: false
            }
        });
    }



    @Cron(CronExpression.EVERY_30_MINUTES)
    async removeNotValidatedTrp() {
        await this.prisma.trip.deleteMany({
            where: {
                expiresAt: { lt: new Date() },
                status: TripStatus.NOT_VALIDATED
            }
        });
    }
}
