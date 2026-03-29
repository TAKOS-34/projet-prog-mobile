import { Controller, Patch, Delete, UseGuards, UseInterceptors, UploadedFile, ParseFilePipe, MaxFileSizeValidator, FileTypeValidator, Body, } from '@nestjs/common';
import { AuthGuard } from 'src/auth/auth.guard';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { ProfileService } from './profile.service';
import { FileInterceptor } from '@nestjs/platform-express';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import type { User } from '@prisma/client';
import { UpdateUserDto } from './dto/updateUser.dto';

@Controller('profile')
export class ProfileController {
    constructor(private readonly profileService: ProfileService) {}



    @UseGuards(AuthGuard)
    @Patch('/avatar')
    @UseInterceptors(FileInterceptor('avatar'))
    updateAvatar(@UploadedFile(
        new ParseFilePipe({
            validators: [
                new MaxFileSizeValidator({ maxSize: 4 * 1000 * 1000 }),
                new FileTypeValidator({
                    fileType: /^image\/(png|jpe?g)(;.*)?$/i,
                    fallbackToMimetype: true,
                }),
            ],
        }),
    ) avatar: Express.Multer.File, @GetUser() user: User): Promise<ResponseMessage> {
        return this.profileService.updateAvatar(avatar, user);
    }



    @UseGuards(AuthGuard)
    @Patch('/infos')
    updateInfos(@Body() updateUserDto: UpdateUserDto, @GetUser() user: User): Promise<ResponseMessage> {
        return this.profileService.updateInfos(updateUserDto, user);
    }



    @UseGuards(AuthGuard)
    @Delete()
    deleteAccount(@GetUser() user: User): Promise<ResponseMessage> {
        return this.profileService.deleteAccount(user);
    }
}
