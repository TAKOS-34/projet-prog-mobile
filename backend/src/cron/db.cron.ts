
import { Injectable } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { PrismaService } from 'src/prisma.service';

@Injectable()
export class DbService {
    constructor(private readonly prisma: PrismaService) {}

    @Cron(CronExpression.EVERY_HOUR)
    async removeExpiredToken() {
        await this.prisma.userToken.deleteMany({
            where: {
                exprirationDate: {
                    lte: new Date(),
                },
            }
        });
    }
}
