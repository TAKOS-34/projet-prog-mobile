import { Controller, Delete, Get, Param, Post, Query, UseGuards } from '@nestjs/common';
import { BookmarkService } from './bookmark.service';
import { AuthGuard } from 'src/auth/auth.guard';
import type { UserSession } from 'src/utils/dto/userSession.dto';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { FeedInfos } from 'src/post/dto/postInfos.dto';

@Controller('bookmark')
export class BookmarkController {
    constructor(private readonly bookmarkService: BookmarkService) {}



    @UseGuards(AuthGuard)
    @Post('/:postId')
    addBookmark(@Param('postId') postId: string, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.bookmarkService.addBookmark(postId, user);
    }



    @UseGuards(AuthGuard)
    @Delete('/:postId')
    deleteBookmark(@Param('postId') postId: string, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.bookmarkService.deleteBookmark(postId, user);
    }



    @UseGuards(AuthGuard)
    @Get()
    getBookmark(@GetUser() user: UserSession, @Query('limit') limit: number = 20, @Query('cursor') cursor?: string): Promise<FeedInfos> {
        return this.bookmarkService.getBookmark(user, limit, cursor);
    }
}
