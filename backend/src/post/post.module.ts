import { Module } from '@nestjs/common';
import { PostController } from './post.controller';
import { PostCommandService } from './post.command.service';
import { AuthModule } from 'src/auth/auth.module';
import { CdnModule } from 'src/cdn/cdn.module';
import { PrismaModule } from 'src/prisma/prisma.module';
import { PostQueryService } from './post.query.service';
import { GroupModule } from 'src/group/group.module';
import { NotificationModule } from 'src/notification/notification.module';

@Module({
    imports: [AuthModule, CdnModule, PrismaModule, GroupModule, NotificationModule],
    controllers: [PostController],
    providers: [PostCommandService, PostQueryService],
})
export class PostModule {}
