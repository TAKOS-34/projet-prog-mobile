import { Injectable, StreamableFile, NotFoundException } from '@nestjs/common';
import { join, basename } from 'path';
import { existsSync, createReadStream } from 'fs';

@Injectable()
export class CdnService {
    private readonly baseUrl: string = process.env.AVATAR_URL ?? './cdn/avatar';

    constructor() {}



    getAvatar(avatar: string): StreamableFile {
        const path = join(process.cwd(), this.baseUrl, basename(avatar));

        if (!existsSync(path)) {
            throw new NotFoundException('Invalid avatar');
        }

        return new StreamableFile(createReadStream(path), {
            type: 'image/jpeg',
            disposition: 'inline',
        });
    }
}
