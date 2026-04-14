import { Controller, Get, Param, Post, Delete, UseGuards, ParseIntPipe } from '@nestjs/common';
import { FollowService } from './follow.service';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { AuthGuard } from 'src/auth/auth.guard';
import type { UserSession } from 'src/utils/dto/userSession.dto';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import { FollowedUserList } from './dto/followedUserList.dto';
import { FollowedGroupList } from './dto/followedGroupList.dto';
import { FollowedTagList } from './dto/followedTagList.dto';

@Controller('follow')
export class FollowController {
    constructor(private readonly followService: FollowService) {}



    @UseGuards(AuthGuard)
    @Get('user')
    getFollowedUser(@GetUser() user: UserSession): Promise<FollowedUserList[]> {
        return this.followService.getFollowedUser(user);
    }



    @UseGuards(AuthGuard)
    @Post('user/:userId')
    addFollowUser(@Param('userId', ParseIntPipe) userId: number, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.followService.addFollowUser(userId, user);
    }



    @UseGuards(AuthGuard)
    @Delete('user/:userId')
    deleteFollowUser(@Param('userId', ParseIntPipe) userId: number, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.followService.deleteFollowUser(userId, user);
    }



    @UseGuards(AuthGuard)
    @Get('group')
    getFollowedGroup(@GetUser() user: UserSession): Promise<FollowedGroupList[]> {
        return this.followService.getFollowedGroup(user);
    }



    @UseGuards(AuthGuard)
    @Post('group/:groupId')
    addFollowGroup(@Param('groupId', ParseIntPipe) groupId: number, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.followService.addFollowGroup(groupId, user);
    }



    @UseGuards(AuthGuard)
    @Delete('group/:groupId')
    deleteFollowGroup(@Param('groupId', ParseIntPipe) groupId: number, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.followService.deleteFollowGroup(groupId, user);
    }



    @UseGuards(AuthGuard)
    @Get('tag')
    getFollowedTag(@GetUser() user: UserSession): Promise<FollowedTagList[]> {
        return this.followService.getFollowedTag(user);
    }



    @UseGuards(AuthGuard)
    @Post('tag/:tagId')
    addFollowTag(@Param('tagId', ParseIntPipe) tagId: number, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.followService.addFollowTag(tagId, user);
    }



    @UseGuards(AuthGuard)
    @Delete('tag/:tagId')
    deleteFollowTag(@Param('tagId', ParseIntPipe) tagId: number, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.followService.deleteFollowTag(tagId, user);
    }
}
