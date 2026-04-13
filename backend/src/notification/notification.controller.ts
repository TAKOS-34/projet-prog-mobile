import { Body, Controller, Get, Patch, Query, UseGuards } from '@nestjs/common';
import { NotificationService } from './notification.service';
import { AuthGuard } from 'src/auth/auth.guard';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { UpdateFcmTokenDto } from './dto/updateFcmToken.dto';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import type { UserSession } from 'src/utils/dto/userSession.dto';
import type { NotificationList } from './dto/notificationList.dto';

@Controller('notification')
export class NotificationController {
    constructor(private readonly notificationService: NotificationService) {}



    @UseGuards(AuthGuard)
    @Get()
    getNotifications(@Query('limit') limit: string = '20', @GetUser() user: UserSession, @Query('cursor') cursor?: string): Promise<NotificationList[]> {
        return this.notificationService.getNotifications(parseInt(limit, 10), user, cursor);
    };



    @UseGuards(AuthGuard)
    @Patch('fcm-token')
    updateFcmToken(@Body() fcmTokenDto: UpdateFcmTokenDto, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.notificationService.updateFcmToken(fcmTokenDto.fcmToken, user?.id);
    }
}
