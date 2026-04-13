import { Module } from '@nestjs/common';
import { NotificationService } from './notification.service';
import { NotificationController } from './notification.controller';
import { AuthModule } from 'src/auth/auth.module';
import { PrismaModule } from 'src/prisma/prisma.module';
import { CdnModule } from 'src/cdn/cdn.module';

@Module({
    imports: [AuthModule, PrismaModule, CdnModule],
    providers: [NotificationService],
    exports: [NotificationService],
    controllers: [NotificationController]
})
export class NotificationModule {}
