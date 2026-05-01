import { Module } from '@nestjs/common';
import { BookmarkController } from './bookmark.controller';
import { BookmarkService } from './bookmark.service';
import { AuthModule } from 'src/auth/auth.module';
import { PrismaModule } from 'src/prisma/prisma.module';
import { CdnModule } from 'src/cdn/cdn.module';

@Module({
  imports: [AuthModule, PrismaModule, CdnModule],
  controllers: [BookmarkController],
  providers: [BookmarkService]
})
export class BookmarkModule {}
