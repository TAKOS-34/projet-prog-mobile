import { Controller, Get, Param, StreamableFile, UseGuards } from '@nestjs/common';
import { CdnService } from './cdn.service';
import { GroupProtectGuard } from 'src/group/group.protect.guard';

@Controller('cdn')
export class CdnController {
    constructor(private readonly cdnService: CdnService) {}



    @Get('avatar/:avatar')
    getAvatar(@Param('avatar') avatar: string): StreamableFile {
        return this.cdnService.getAvatar(avatar);
    }



    @UseGuards(GroupProtectGuard)
    @Get('post/:postId')
    getPost(@Param('postId') postId: string): StreamableFile {
        return this.cdnService.getPost(postId);
    }



    @Get('group/:avatar')
    getGroupAvatar(@Param('avatar') avatar: string): StreamableFile {
        return this.cdnService.getGroupAvatar(avatar);
    }
}
