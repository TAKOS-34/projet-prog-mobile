import { PipeTransform, Injectable, BadRequestException } from '@nestjs/common';

@Injectable()
export class PostFilesValidatorPipe implements PipeTransform {
    transform(files: { image?: Express.Multer.File[], audio?: Express.Multer.File[] }) {
        const image = files?.image?.[0];
        const audio = files?.audio?.[0];

        if (!image) {
            throw new BadRequestException('Image is required');
        }
        if (image.size > 5 * 1024 * 1024) {
            throw new BadRequestException('Image too large (max 5MB)');
        }
        if (!image.mimetype.match(/^image\/(png|jpe?g)$/)) {
            throw new BadRequestException('Invalid image format (PNG/JPG only)');
        }

        if (audio) {
            if (audio.size > 2 * 1024 * 1024) {
                throw new BadRequestException('Audio too large (max 2MB)');
            }
            if (!audio.mimetype.match(/^audio\/(mpeg|mp4|aac|x-m4a)$/)) {
                throw new BadRequestException('Invalid audio format (MP3/M4A only)');
            }
        }

        return { image, audio };
    }
}