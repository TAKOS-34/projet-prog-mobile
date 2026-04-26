import { Body, Controller, Delete, Get, Param, Patch, Post, Query, UseGuards } from '@nestjs/common';
import { NotificationService } from './notification.service';
import { AuthGuard } from 'src/auth/auth.guard';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { UpdateFcmTokenDto } from './dto/updateFcmToken.dto';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import type { UserSession } from 'src/utils/dto/userSession.dto';
import type { NotificationList } from './dto/notificationList.dto';
import { ReadNotificationListDto } from './dto/readNotificationList.dto';
import { UserFollowingList } from './dto/userFollowingList.dto';

@Controller('notification')
export class NotificationController {
    constructor(private readonly notificationService: NotificationService) {}



    @UseGuards(AuthGuard)
    @Get()
    getNotifications(@Query('limit') limit: string = '20', @GetUser() user: UserSession, @Query('cursor') cursor?: number): Promise<NotificationList[]> {
        return this.notificationService.getNotifications(parseInt(limit, 10), user, cursor);
    }



    @UseGuards(AuthGuard)
    @Get('following')
    getUserFollowing(@GetUser() user: UserSession): Promise<UserFollowingList[]> {
        return this.notificationService.getUserFollowing(user);
    }



    @UseGuards(AuthGuard)
    @Get('new')
    getNewNotificationNumber(@GetUser() user: UserSession): Promise<number> {
        return this.notificationService.getNewNotificationNumber(user);
    }



    @UseGuards(AuthGuard)
    @Patch('mark-as-read')
    markAsRead(@Body() readNotificationListDto: ReadNotificationListDto, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.notificationService.markAsRead(readNotificationListDto.notificationIds, user);
    }



    @UseGuards(AuthGuard)
    @Patch('fcm-token')
    updateFcmToken(@Body() fcmTokenDto: UpdateFcmTokenDto, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.notificationService.updateFcmToken(fcmTokenDto.fcmToken, user?.id);
    }



    @UseGuards(AuthGuard)
    @Post('/user/:userId')
    addUserFollow(@GetUser() user: UserSession, @Param('userId') userId: number): Promise<ResponseMessage> {
        return this.notificationService.addUserFollow(user, userId);
    }

    @UseGuards(AuthGuard)
    @Delete('/user/:userId')
    deleteUserFollow(@GetUser() user: UserSession, @Param('userId') userId: number): Promise<ResponseMessage> {
        return this.notificationService.deleteUserFollow(user, userId);
    }

    @UseGuards(AuthGuard)
    @Post('/group/:groupId')
    addGroupFollow(@GetUser() user: UserSession, @Param('groupId') groupId: number): Promise<ResponseMessage> {
        return this.notificationService.addGroupFollow(user, groupId);
    }

    @UseGuards(AuthGuard)
    @Delete('/group/:groupId')
    deleteGroupFollow(@GetUser() user: UserSession, @Param('groupId') groupId: number): Promise<ResponseMessage> {
        return this.notificationService.deleteGroupFollow(user, groupId);
    }

    @UseGuards(AuthGuard)
    @Post('/tag/:tagId')
    addTagFollow(@GetUser() user: UserSession, @Param('tagId') tagId: number): Promise<ResponseMessage> {
        return this.notificationService.addTagFollow(user, tagId);
    }

    @UseGuards(AuthGuard)
    @Delete('/tag/:tagId')
    deleteTagFollow(@GetUser() user: UserSession, @Param('tagId') tagId: number): Promise<ResponseMessage> {
        return this.notificationService.deleteTagFollow(user, tagId);
    }
}
