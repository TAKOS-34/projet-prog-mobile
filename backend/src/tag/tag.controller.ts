import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { TagService } from './tag.service';
import { AuthGuard } from 'src/auth/auth.guard';

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
}
