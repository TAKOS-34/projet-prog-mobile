import { Module } from '@nestjs/common';
import { FollowController } from './follow.controller';
import { FollowService } from './follow.service';
import { PrismaModule } from 'src/prisma/prisma.module';
import { AuthModule } from 'src/auth/auth.module';
import { CdnModule } from 'src/cdn/cdn.module';

@Module({
    imports: [PrismaModule, AuthModule, CdnModule],
    controllers: [FollowController],
    providers: [FollowService]
})
export class FollowModule {}
