import { Controller, Delete, Get, Param, Post, Query, UseGuards } from '@nestjs/common';
import { BookmarkService } from './bookmark.service';
import { AuthGuard } from 'src/auth/auth.guard';
import type { UserSession } from 'src/utils/dto/userSession.dto';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { PostsInfos } from 'src/post/dto/postInfos.dto';
import { TripInfos } from 'src/trip/dto/tripInfos.dto';

@Controller('bookmark')
export class BookmarkController {
    constructor(private readonly bookmarkService: BookmarkService) {}



    @UseGuards(AuthGuard)
    @Post('/post/:postId')
    addPostBookmark(@Param('postId') postId: string, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.bookmarkService.addPostBookmark(postId, user);
    }



    @UseGuards(AuthGuard)
    @Delete('/post/:postId')
    deletePostBookmark(@Param('postId') postId: string, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.bookmarkService.deletePostBookmark(postId, user);
    }



    @UseGuards(AuthGuard)
    @Get('/post')
    getPostBookmark(@GetUser() user: UserSession, @Query('limit') limit: number = 20, @Query('cursor') cursor?: string): Promise<PostsInfos> {
        return this.bookmarkService.getPostBookmark(user, limit, cursor);
    }



    @UseGuards(AuthGuard)
    @Post('/trip/:tripId')
    addTripBookmark(@Param('tripId') tripId: number, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.bookmarkService.addTripBookmark(tripId, user);
    }



    @UseGuards(AuthGuard)
    @Delete('/trip/:tripId')
    deleteTripBookmark(@Param('tripId') tripId: number, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.bookmarkService.deleteTripBookmark(tripId, user);
    }



    @UseGuards(AuthGuard)
    @Get('/trip')
    getTripBookmark(@GetUser() user: UserSession, @Query('limit') limit: number = 20, @Query('cursor') cursor?: string): Promise<TripInfos> {
        return this.bookmarkService.getTripBookmark(user, limit, cursor);
    }
}
