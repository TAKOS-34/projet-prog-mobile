import { Body, Controller, Delete, FileTypeValidator, Get, HttpCode, MaxFileSizeValidator, Param, ParseFilePipe, Patch, Post, UploadedFile, UseGuards, UseInterceptors } from '@nestjs/common';
import { GroupAdminService } from './group-admin.service';
import { GroupAdminGuard } from './group-admin.guard';
import { GetGroup } from 'src/utils/decorator/get-group.decorator';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { AuthGuard } from 'src/auth/auth.guard';
import type { Group, User } from '@prisma/client';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import { UpdateGroupDto } from './dto/updateGroup.dto';
import { FileInterceptor } from '@nestjs/platform-express';

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



    @Patch('/:groupId')
    updateGroup(@Param('groupId') groupId: number, @Body() updateGroupDto: UpdateGroupDto, @GetGroup() group: Group): Promise<ResponseMessage> {
        return this.groupAdminService.updateGroup(updateGroupDto, group);
    }



    @Patch('/avatar/:groupId')
    @UseInterceptors(FileInterceptor('avatar'))
    updateGroupAvatar(@Param('groupId') groupId: number, @GetGroup() group: Group, @UploadedFile(
            new ParseFilePipe({
                validators: [
                    new MaxFileSizeValidator({ maxSize: 2 * 1000 * 1000 }),
                    new FileTypeValidator({
                        fileType: /^image\/(png|jpe?g)(;.*)?$/i,
                        fallbackToMimetype: true,
                    }),
                ]
            })
        ) avatar: Express.Multer.File): Promise<ResponseMessage> {
        return this.groupAdminService.updateGroupAvatar(group, avatar);
    }



    @Patch('/transfer-admin-role/:groupId/:userId')
    transferAdminRole(@Param('groupId') groupId: number, @Param('userId') userId: number, @GetGroup() group: Group, @GetUser() user: User): Promise<ResponseMessage> {
        return this.groupAdminService.transferAdminRole(userId, group, user);
    }



    @Delete('/:groupId')
    deleteGroup(@Param('groupId') groupId: number, @GetGroup() group: Group): Promise<ResponseMessage> {
        return this.groupAdminService.deleteGroup(group);
    }
}
