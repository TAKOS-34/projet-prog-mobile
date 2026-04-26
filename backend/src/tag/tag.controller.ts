import { Controller, FileTypeValidator, Get, MaxFileSizeValidator, Param, ParseFilePipe, Post, Query, UploadedFile, UseGuards, UseInterceptors } from '@nestjs/common';
import { TagService } from './tag.service';
import { AuthGuard } from 'src/auth/auth.guard';
import { FileInterceptor } from '@nestjs/platform-express';
import { tag } from './dto/tag.dto';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import type { UserSession } from 'src/utils/dto/userSession.dto';

@Controller('tag')
export class TagController {
    constructor(private readonly tagService: TagService) {}



    @UseGuards(AuthGuard)
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
    @Get('/:tag')
    getTag(@Param('tag') tagId: string, @GetUser() user: UserSession): Promise<tag> {
        return this.tagService.getTag(tagId, user);
    }



    @UseGuards(AuthGuard)
    @Post('/ia-suggestions')
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
