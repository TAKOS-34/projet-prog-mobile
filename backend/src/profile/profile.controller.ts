import { Param, Controller, Get, Patch, Delete, UseGuards, UseInterceptors, UploadedFile, ParseFilePipe, MaxFileSizeValidator, FileTypeValidator, Body, ParseIntPipe, } from '@nestjs/common';
import { AuthGuard } from 'src/auth/auth.guard';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { ProfileService } from './profile.service';
import { FileInterceptor } from '@nestjs/platform-express';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import type { UserSession } from 'src/utils/dto/userSession.dto';
import { UpdateUserDto } from './dto/updateUser.dto';
import { Token } from './dto/token.dto';
import type { UserProfile } from './dto/userProfile.dto';
import { UserPublicProfile } from './dto/userPublicProfile.dto';
import { UserGroup } from './dto/userGroup.dto';

@Controller('profile')
export class ProfileController {
    constructor(private readonly profileService: ProfileService) {}



    @UseGuards(AuthGuard)
    @Get()
    getProfile(@GetUser() user: UserSession): UserProfile {
        return this.profileService.getProfile(user);
    }

    @UseGuards(AuthGuard)
    @Get('/groups')
    getGroups(@GetUser() user: UserSession): Promise<UserGroup[]> {
        return this.profileService.getGroups(user);
    }

    @UseGuards(AuthGuard)
    @Get('tokens')
    getTokens(@GetUser() user: UserSession): Promise<Array<Token>> {
        return this.profileService.getTokens(user);
    }

    @Get('/:userId')
    getPublicProfile(@Param('userId', ParseIntPipe) userId: number): Promise<UserPublicProfile> {
        return this.profileService.getPublicProfile(userId);
    }



    @UseGuards(AuthGuard)
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
    ) avatar: Express.Multer.File, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.profileService.updateAvatar(avatar, user);
    }



    @UseGuards(AuthGuard)
    @Patch('/infos')
    updateInfos(@Body() updateUserDto: UpdateUserDto, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.profileService.updateInfos(updateUserDto, user);
    }



    @UseGuards(AuthGuard)
    @Delete()
    deleteAccount(@GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.profileService.deleteAccount(user);
    }



    @UseGuards(AuthGuard)
    @Delete('token/:tokenId')
    deleteToken(@Param('tokenId', ParseIntPipe) tokenId: number, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.profileService.deleteToken(tokenId, user);
    }
}
