import { Body, Controller, Get, Param, Post, Query, UseGuards } from '@nestjs/common';
import { TripCreationService } from './trip.creation.service';
import { AuthGuard } from 'src/auth/auth.guard';
import { SuggestTripDto } from './dto/suggestTrip.dto';
import type { UserSession } from 'src/utils/dto/userSession.dto';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import { TripDto, TripInfos, TripSuggest } from './dto/tripInfos.dto';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { TripService } from './trip.service';
import { AuthOptionalGuard } from 'src/auth/auth.optionnal.guard';
import { ConfirmTripDto } from './dto/confirmTrip.dto';

@Controller('trip')
export class TripController {
    constructor(
        private readonly tripCreationService: TripCreationService,
        private readonly tripService: TripService
    ) {}



    @UseGuards(AuthGuard)
    @Post('/suggest')
    suggestTrips(@Body() suggestTripDto: SuggestTripDto, @GetUser() user: UserSession): Promise<TripSuggest> {
        return this.tripCreationService.suggestTrips(suggestTripDto, user);
    }



    @UseGuards(AuthGuard)
    @Post('/confirm/:tripId')
    confirmTrip(@Param('tripId') tripId: number, @Body() confirmTripDto: ConfirmTripDto, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.tripService.confirmTrip(tripId, confirmTripDto, user)
    }



    @UseGuards(AuthGuard)
    @Get('my-trips')
    getMyTrips(@GetUser() user: UserSession, @Query('limit') limit: number = 20, @Query('cursor') cursor?: number): Promise<TripInfos> {
        return this.tripService.getMyTrips(user, limit, cursor);
    }



    @UseGuards(AuthOptionalGuard)
    @Get()
    getTrips(@GetUser() user?: UserSession, @Query('limit') limit: number = 20, @Query('cursor') cursor?: number): Promise<TripInfos> {
        return this.tripService.getTrips(limit, cursor, user?.id);
    }



    @UseGuards(AuthOptionalGuard)
    @Get('/:tripId')
    getTrip(@Param('tripId') tripId: number, @GetUser() user?: UserSession): Promise<TripDto> {
        return this.tripService.getTrip(tripId, user?.id);
    }
}
