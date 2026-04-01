import { Controller, Get, Param, StreamableFile } from '@nestjs/common';
import { CdnService } from './cdn.service';

@Controller('cdn')
export class CdnController {
    constructor(private readonly cdnService: CdnService) {}



    @Get('avatar/:avatar')
    getAvatar(@Param('avatar') avatar: string): StreamableFile {
        return this.cdnService.getAvatar(avatar);
    }



    @Get('post/:image')
    getPost(@Param('image') image: string): StreamableFile {
        return this.cdnService.getPost(image);
    }



    @Get('group/:avatar')
    getGroupAvatar(@Param('image') image: string): StreamableFile {
        return this.cdnService.getGroupAvatar(image);
    }
}
