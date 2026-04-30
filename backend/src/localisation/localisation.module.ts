import { Module } from '@nestjs/common';
import { LocalisationController } from './localisation.controller';
import { LocalisationService } from './localisation.service';
import { AuthModule } from 'src/auth/auth.module';
import { PrismaModule } from 'src/prisma/prisma.module';

@Module({
  imports: [AuthModule, PrismaModule],
  controllers: [LocalisationController],
  providers: [LocalisationService]
})
export class LocalisationModule {}
