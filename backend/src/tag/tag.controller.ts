import { Controller, FileTypeValidator, Get, MaxFileSizeValidator, ParseFilePipe, Query, UploadedFile, UseGuards, UseInterceptors } from '@nestjs/common';
import { TagService } from './tag.service';
import { AuthGuard } from 'src/auth/auth.guard';
import { FileInterceptor } from '@nestjs/platform-express';

@Controller('tag')
export class TagController {
    constructor(private readonly tagService: TagService) {}



    @Get('popular')
    getPopularTag(): Promise<string[]> {
        return this.tagService.getPopularTag();
    }



    @UseGuards(AuthGuard)
    @Get('suggest')
    suggestTag(@Query('tag') tag: string): Promise<string[]> {
        return this.tagService.suggestTag(tag);
    }



    @UseGuards(AuthGuard)
    @Get('/ia-suggestions')
    @UseInterceptors(FileInterceptor('post'))
    iaSuggestions(@UploadedFile(
        new ParseFilePipe({
            validators: [
                new MaxFileSizeValidator({ maxSize: 5 * 1000 * 1000 }),
                new FileTypeValidator({
                    fileType: /^image\/(png|jpe?g)(;.*)?$/i,
                    fallbackToMimetype: true,
                }),
            ],
        }),
    ) post: Express.Multer.File): Promise<string[]> {
        return this.tagService.iaSuggestions(post);
    }
}
