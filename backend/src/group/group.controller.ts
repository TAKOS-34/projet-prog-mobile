import { Controller, Post, Get, Param, UseGuards, Body, UseInterceptors, UploadedFile, ParseFilePipe, MaxFileSizeValidator, FileTypeValidator, HttpCode, Delete, ParseIntPipe } from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { GroupService } from './group.service';
import { AuthGuard } from 'src/auth/auth.guard';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import { CreateGroupDto } from './dto/createGroup.dto';
import type { UserSession } from 'src/utils/dto/userSession.dto';
import { GroupProtectGuard } from './group.protect.guard';
import { UserList } from 'src/group/dto/userList.dto';
import { GroupInfos } from './dto/groupInfos.dto';
import { AuthOptionalGuard } from 'src/auth/auth.optionnal.guard';
import { GetAnonymous } from 'src/utils/decorator/get-anonymous.decorator';
import { PostInfos } from 'src/post/dto/postInfos.dto';

@Controller('group')
export class GroupController {
    constructor(private readonly groupService: GroupService) {}



    @UseGuards(AuthOptionalGuard)
    @Get('/:groupId')
    getGroupInfos(@Param('groupId') groupId: number, @GetUser() user?: UserSession): Promise<GroupInfos> {
        return this.groupService.getGroupInfos(groupId, user);
    }

    @UseGuards(GroupProtectGuard)
    @Get('/:groupId/members')
    getGroupMembers(@Param('groupId') groupId: number): Promise<UserList[]> {
        return this.groupService.getGroupMembers(groupId);
    }

    @UseGuards(GroupProtectGuard, AuthOptionalGuard)
    @Get('/:groupId/posts')
    getGroupPosts(@Param('groupId') groupId: number, @GetUser() user?: UserSession, @GetAnonymous() anonymous?: string): Promise<PostInfos[]> {
        return this.groupService.getGroupPosts(groupId);
    }



    @UseGuards(AuthGuard)
    @Post()
    @UseInterceptors(FileInterceptor('avatar'))
    createGroup(@Body() createGroupDto: CreateGroupDto, @GetUser() user: UserSession, @UploadedFile(
        new ParseFilePipe({
            validators: [
                new MaxFileSizeValidator({ maxSize: 2 * 1000 * 1000 }),
                new FileTypeValidator({
                    fileType: /^image\/(png|jpe?g)(;.*)?$/i,
                    fallbackToMimetype: true,
                }),
            ],
            fileIsRequired: false,
        }),
    ) avatar?: Express.Multer.File): Promise<ResponseMessage> {
        return this.groupService.createGroup(createGroupDto, user, avatar);
    }



    @UseGuards(AuthGuard)
    @HttpCode(200)
    @Post('request-to-join/:groupId')
    requestToJoin(@Param('groupId') groupId: number, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.groupService.requestToJoin(groupId, user);
    }



    @UseGuards(AuthGuard)
    @HttpCode(200)
    @Delete('quit/:groupId')
    quit(@Param('groupId') groupId: number, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.groupService.quit(groupId, user);
    }
}
