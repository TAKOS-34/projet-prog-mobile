import { Module } from '@nestjs/common';
import { TripController } from './trip.controller';
import { TripService } from './trip.service';
import { PrismaModule } from 'src/prisma/prisma.module';
import { AuthModule } from 'src/auth/auth.module';
import { LocalisationModule } from 'src/localisation/localisation.module';

@Module({
  imports: [PrismaModule, AuthModule, LocalisationModule],
  controllers: [TripController],
  providers: [TripService]
})
export class TripModule {}
