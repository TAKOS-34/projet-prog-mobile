import { Controller, Delete, Get, HttpCode, Param, Patch, Post, UseGuards } from '@nestjs/common';
import { GroupAdminService } from './group-admin.service';
import { GroupAdminGuard } from './group-admin.guard';
import { GetGroup } from 'src/utils/decorator/get-group.decorator';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { AuthGuard } from 'src/auth/auth.guard';
import type { Group, User } from '@prisma/client';
import { GetUser } from 'src/utils/decorator/get-user.decorator';

@UseGuards(AuthGuard, GroupAdminGuard)
@Controller('group/admin')
export class GroupAdminController {
    constructor(private readonly groupAdminService: GroupAdminService) {}



    @Get('/:groupId/list/request')
    listRequest(@Param('groupId') groupId: number): Promise<any> {
        return this.groupAdminService.listRequest(groupId);
    }



    @Get('/:groupId/list/ban')
    listBan(@Param('groupId') groupId: number): Promise<any> {
        return this.groupAdminService.listBan(groupId);
    }



    @HttpCode(200)
    @Post('accept/:groupId/:userId')
    accept(@Param('groupId') groupId: number, @Param('userId') userId: number, @GetGroup() group: Group): Promise<ResponseMessage> {
        return this.groupAdminService.accept(userId, group);
    }



    @HttpCode(200)
    @Post('refuse/:groupId/:userId')
    refuse(@Param('groupId') groupId: number, @Param('userId') userId: number, @GetGroup() group: Group): Promise<ResponseMessage> {
        return this.groupAdminService.refuse(userId, group);
    }



    @HttpCode(200)
    @Post('ban/:groupId/:userId')
    ban(@Param('groupId') groupId: number, @Param('userId') userId: number, @GetGroup() group: Group): Promise<ResponseMessage> {
        return this.groupAdminService.ban(userId, group);
    }



    @Delete('deban/:groupId/:userId')
    deban(@Param('groupId') groupId: number, @Param('userId') userId: number, @GetGroup() group: Group): Promise<ResponseMessage> {
        return this.groupAdminService.deban(userId, group);
    }



    @Patch('/transfer-admin-role/:groupId/:userId')
    transferAdminRole(@Param('groupId') groupId: number, @Param('userId') userId: number, @GetGroup() group: Group, @GetUser() user: User): Promise<ResponseMessage> {
        return this.groupAdminService.transferAdminRole(userId, group, user);
    }



    @Delete('/:groupId')
    delete(@Param('groupId') groupId: number, @GetGroup() group: Group): Promise<ResponseMessage> {
        return this.groupAdminService.delete(group);
    }
}
