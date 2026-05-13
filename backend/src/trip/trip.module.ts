import { Module } from '@nestjs/common';
import { TripController } from './trip.controller';
import { TripCreationService } from './trip.creation.service';
import { PrismaModule } from 'src/prisma/prisma.module';
import { AuthModule } from 'src/auth/auth.module';
import { LocalisationModule } from 'src/localisation/localisation.module';

@Module({
  imports: [PrismaModule, AuthModule, LocalisationModule],
  controllers: [TripController],
  providers: [TripCreationService]
})
export class TripModule {}
