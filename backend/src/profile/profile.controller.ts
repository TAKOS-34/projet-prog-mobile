import { Param, Controller, Get, Patch, Delete, UseGuards, UseInterceptors, UploadedFile, ParseFilePipe, MaxFileSizeValidator, FileTypeValidator, Body, } from '@nestjs/common';
import { AuthGuard } from 'src/auth/auth.guard';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { ProfileService } from './profile.service';
import { FileInterceptor } from '@nestjs/platform-express';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import type { User } from '@prisma/client';
import { UpdateUserDto } from './dto/updateUser.dto';
import { Token } from './dto/token.dto';
import type { UserProfile } from './dto/userProfile.dto';

@UseGuards(AuthGuard)
@Controller('profile')
export class ProfileController {
    constructor(private readonly profileService: ProfileService) {}



    @Get()
    getProfile(@GetUser() user: User): UserProfile {
        return this.profileService.getProfile(user);
    }



    @Patch('/avatar')
    @UseInterceptors(FileInterceptor('avatar'))
    updateAvatar(@UploadedFile(
        new ParseFilePipe({
            validators: [
                new MaxFileSizeValidator({ maxSize: 2 * 1000 * 1000 }),
                new FileTypeValidator({
                    fileType: /^image\/(png|jpe?g)(;.*)?$/i,
                    fallbackToMimetype: true,
                }),
            ],
        }),
    ) avatar: Express.Multer.File, @GetUser() user: User): Promise<ResponseMessage> {
        return this.profileService.updateAvatar(avatar, user);
    }



    @Patch('/infos')
    updateInfos(@Body() updateUserDto: UpdateUserDto, @GetUser() user: User): Promise<ResponseMessage> {
        return this.profileService.updateInfos(updateUserDto, user);
    }



    @Delete()
    deleteAccount(@GetUser() user: User): Promise<ResponseMessage> {
        return this.profileService.deleteAccount(user);
    }



    @Get('tokens')
    getTokens(@GetUser() user: User): Promise<Array<Token>> {
        return this.profileService.getTokens(user);
    }



    @Delete('token/:tokenId')
    deleteToken(@Param('tokenId') tokenId: string, @GetUser() user: User): Promise<ResponseMessage> {
        return this.profileService.deleteToken(Number(tokenId), user);
    }
}
