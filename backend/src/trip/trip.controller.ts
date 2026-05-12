import { Body, Controller, Post, UseGuards } from '@nestjs/common';
import { TripService } from './trip.service';
import { AuthGuard } from 'src/auth/auth.guard';
import { SuggestTripDto } from './dto/suggestTrip.dto';
import type { UserSession } from 'src/utils/dto/userSession.dto';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import { TripSuggestResponse } from './dto/tripInfos.dto';

@Controller('trip')
export class TripController {
    constructor(private readonly tripService: TripService) {}



    @UseGuards(AuthGuard)
    @Post('/suggest')
    suggestTrips(@Body() suggestTripDto: SuggestTripDto, @GetUser() user: UserSession): Promise<TripSuggestResponse> {
        return this.tripService.suggestTrips(suggestTripDto, user);
    }
}
