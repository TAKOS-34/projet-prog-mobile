import { Module } from '@nestjs/common';
import { PostController } from './post.controller';
import { PostService } from './post.service';
import { CdnService } from 'src/cdn/cdn.service';
import { PrismaService } from 'src/prisma/prisma.service';
import { AuthModule } from 'src/auth/auth.module';

@Module({
    imports: [AuthModule],
    controllers: [PostController],
    providers: [PostService, CdnService, PrismaService]
})
export class PostModule {}
