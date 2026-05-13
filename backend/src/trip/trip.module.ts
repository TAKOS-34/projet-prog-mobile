import { Module } from '@nestjs/common';
import { TripController } from './trip.controller';
import { TripCreationService } from './trip.creation.service';
import { PrismaModule } from 'src/prisma/prisma.module';
import { AuthModule } from 'src/auth/auth.module';
import { LocalisationModule } from 'src/localisation/localisation.module';
import { TripService } from './trip.service';
import { CdnModule } from 'src/cdn/cdn.module';

@Module({
  imports: [PrismaModule, AuthModule, LocalisationModule, CdnModule],
  controllers: [TripController],
  providers: [TripCreationService, TripService]
})
export class TripModule {}
