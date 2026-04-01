import { Module } from '@nestjs/common';
import { PostController } from './post.controller';
import { PostService } from './post.service';
import { AuthModule } from 'src/auth/auth.module';
import { CdnModule } from 'src/cdn/cdn.module';
import { PrismaModule } from 'src/prisma/prisma.module';

@Module({
    imports: [AuthModule, CdnModule, PrismaModule],
    controllers: [PostController],
    providers: [PostService],
})
export class PostModule {}
