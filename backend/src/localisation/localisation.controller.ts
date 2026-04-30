import { Controller, Get, Param, UseGuards } from '@nestjs/common';
import { LocalisationService } from './localisation.service';
import { AuthGuard } from 'src/auth/auth.guard';
import type { UserSession } from 'src/utils/dto/userSession.dto';
import { localisation } from './dto/localisation.dto';
import { GetUser } from 'src/utils/decorator/get-user.decorator';

@Controller('localisation')
export class LocalisationController {
    constructor(private readonly localisationService: LocalisationService) {}



    @UseGuards(AuthGuard)
    @Get(':localisation')
    getLocalisation(@Param('localisation') localisation: string, @GetUser() user: UserSession): Promise<localisation> {
        return this.localisationService.getLocalisation(localisation, user);
    }
}
