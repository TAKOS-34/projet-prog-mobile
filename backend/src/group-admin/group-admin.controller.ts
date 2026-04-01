import { Controller, Delete, HttpCode, Param, Post, UseGuards } from '@nestjs/common';
import { GroupAdminService } from './group-admin.service';
import { GroupAdminGuard } from './group-admin.guard';
import { GetGroup } from 'src/utils/decorator/get-group.decorator';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { AuthGuard } from 'src/auth/auth.guard';
import type { Group } from '@prisma/client';

@Controller('group/admin')
export class GroupAdminController {
    constructor(private readonly groupAdminService: GroupAdminService) {}


    @UseGuards(AuthGuard, GroupAdminGuard)
    @HttpCode(200)
    @Post('accept/:groupId/:userId')
    accept(@Param('groupId') groupId: number, @Param('userId') userId: number, @GetGroup() group: Group): Promise<ResponseMessage> {
        return this.groupAdminService.accept(userId, group);
    }



    @UseGuards(AuthGuard, GroupAdminGuard)
    @HttpCode(200)
    @Post('refuse/:groupId/:userId')
    refuse(@Param('groupId') groupId: number, @Param('userId') userId: number, @GetGroup() group: Group): Promise<ResponseMessage> {
        return this.groupAdminService.refuse(userId, group);
    }



    @UseGuards(AuthGuard, GroupAdminGuard)
    @HttpCode(200)
    @Post('ban/:groupId/:userId')
    ban(@Param('groupId') groupId: number, @Param('userId') userId: number, @GetGroup() group: Group): Promise<ResponseMessage> {
        return this.groupAdminService.ban(userId, group);
    }



    @UseGuards(AuthGuard, GroupAdminGuard)
    @HttpCode(200)
    @Delete('deban/:groupId/:userId')
    deban(@Param('groupId') groupId: number, @Param('userId') userId: number, @GetGroup() group: Group): Promise<ResponseMessage> {
        return this.groupAdminService.deban(userId, group);
    }
}
