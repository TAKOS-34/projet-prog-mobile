import { Controller, Post, Get, Param, UseGuards, Body, UseInterceptors, UploadedFile, ParseFilePipe, MaxFileSizeValidator, FileTypeValidator, HttpCode, Delete, ParseIntPipe } from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { GroupService } from './group.service';
import { AuthGuard } from 'src/auth/auth.guard';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import type { User, Group } from '@prisma/client';
import { CreateGroupDto } from './dto/createGroup.dto';

@Controller('group')
export class GroupController {
    constructor(private readonly groupService: GroupService) {}



    @UseGuards(AuthGuard)
    @Post()
    @UseInterceptors(FileInterceptor('avatar'))
    createGroup(@Body() createGroupDto: CreateGroupDto, @GetUser() user: User, @UploadedFile(
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
    requestToJoin(@Param('groupId') groupId: number, @GetUser() user: User): Promise<ResponseMessage> {
        return this.groupService.requestToJoin(groupId, user);
    }



    @UseGuards(AuthGuard)
    @HttpCode(200)
    @Delete('quit/:groupId')
    quit(@Param('groupId') groupId: number, @GetUser() user: User): Promise<ResponseMessage> {
        return this.groupService.quit(groupId, user);
    }
}
