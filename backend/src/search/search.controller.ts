import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { SearchService } from './search.service';
import { GroupSearch } from './dto/groupSearch.dto';
import { AuthOptionalGuard } from 'src/auth/auth.optionnal.guard';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import type { UserSession } from 'src/utils/dto/userSession.dto';
import { GetAnonymous } from 'src/utils/decorator/get-anonymous.decorator';
import { PostSearch } from './dto/postSearch.dto';

@Controller('search')
export class SearchController {
    constructor(private readonly searchService: SearchService) {}



    @Get('/groups')
    searchGroups(@Query('name') name: string): Promise<GroupSearch[]> {
        return this.searchService.searchGroups(name);
    }



    @UseGuards(AuthOptionalGuard)
    @Get('/posts')
    searchPosts(
        @Query('q') q?: string,
        @Query('tag') tag?: string,
        @Query('cursor') cursor?: string,
        @GetUser() user?: UserSession,
        @GetAnonymous() anonymous?: string
    ): Promise<PostSearch[]> {
        return this.searchService.searchPosts(q, tag, cursor, user?.id, anonymous);
    }
}
