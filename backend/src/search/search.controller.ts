import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { SearchService } from './search.service';
import { GroupSearch } from './dto/groupSearch.dto';
import { AuthOptionalGuard } from 'src/auth/auth.optionnal.guard';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import type { UserSession } from 'src/utils/dto/userSession.dto';

@Controller('search')
export class SearchController {
    constructor(private readonly searchService: SearchService) {}



    @UseGuards(AuthOptionalGuard)
    @Get('/groups')
    searchGroups(
        @Query('name') name: string,
        @GetUser() user?: UserSession
    ): Promise<GroupSearch[]> {
        return this.searchService.searchGroups(name, user?.id);
    }
}
