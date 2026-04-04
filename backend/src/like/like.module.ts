import { Module } from '@nestjs/common';
import { LikeController } from './like.controller';
import { LikeService } from './like.service';
import { PrismaModule } from 'src/prisma/prisma.module';
import { AuthModule } from 'src/auth/auth.module';
import { AuthLikeGuard } from './auth.like.guard';
import { GroupModule } from 'src/group/group.module';

@Module({
    imports: [PrismaModule, AuthModule, GroupModule],
    controllers: [LikeController],
    providers: [LikeService, AuthLikeGuard]
})
export class LikeModule {}
