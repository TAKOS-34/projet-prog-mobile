import { Injectable, StreamableFile, NotFoundException } from '@nestjs/common';
import { join, basename } from 'path';
import { existsSync, createReadStream } from 'fs';

@Injectable()
export class CdnService {
    private readonly avatarDir: string = process.env.AVATAR_DIR ?? 'cdn/avatar';
    private readonly avatarUrl: string = process.env.AVATAR_URL ?? '';
    private readonly defaultAvatar: string = 'default.jpg';

    private readonly postDir: string = process.env.POST_DIR ?? 'cdn/post';
    private readonly postUrl: string = process.env.POST_URL ?? '';

    constructor() {}


    getAvatarPath(avatar: string): string {
        return join(process.cwd(), this.avatarDir, basename(avatar));
    }

    getAvatarUrl(avatar: string | null): string {
        return this.avatarUrl + (avatar ?? this.defaultAvatar);
    }

    getPostPath(image: string): string {
        return join(process.cwd(), this.postDir, basename(image));
    }

    getPostUrl(postId: string, imageExt: string): string {
        return this.postUrl + postId + '.' + imageExt;
    }



    getAvatar(avatar: string): StreamableFile {
        const path = this.getAvatarPath(avatar);

        if (!existsSync(path)) {
            throw new NotFoundException('Invalid avatar');
        }

        return new StreamableFile(createReadStream(path), {
            type: 'image/jpeg',
            disposition: 'inline',
        });
    }



    getPost(image: string): StreamableFile {
        const path = this.getPostPath(image);

        if (!existsSync(path)) {
            throw new NotFoundException('Invalid post');
        }

        return new StreamableFile(createReadStream(path), {
            type: 'image/jpeg',
            disposition: 'inline',
        });
    }
}
