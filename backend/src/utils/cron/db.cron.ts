import { Injectable } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { PrismaService } from 'src/prisma/prisma.service';

@Injectable()
export class DbService {
    constructor(private readonly prisma: PrismaService) {}

    @Cron(CronExpression.EVERY_HOUR)
    async removeExpiredToken() {
        await this.prisma.userToken.deleteMany({
            where: {
                exprirationDate: { lte: new Date() }
            }
        });
    }



    @Cron(CronExpression.EVERY_1ST_DAY_OF_MONTH_AT_MIDNIGHT)
    async removeExpiredUser() {
        const expirationLimit = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);

        await this.prisma.user.deleteMany({
            where: {
                creationDate: { lte: expirationLimit },
                isEmailVerified: true
            }
        });
    }
}
