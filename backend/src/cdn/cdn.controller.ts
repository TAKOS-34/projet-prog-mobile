import { Controller, Get, Param, StreamableFile, NotFoundException } from '@nestjs/common';
import { createReadStream, existsSync } from 'fs';
import { join } from 'path';
import { CdnService } from './cdn.service';

@Controller('cdn')
export class CdnController {
    constructor(private readonly cdnService: CdnService) {}



    @Get('avatar/:avatar')
    getAvatar(@Param('avatar') avatar: string): StreamableFile {
        return this.cdnService.getAvatar(avatar);
    }
}
