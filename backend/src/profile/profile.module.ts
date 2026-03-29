import { Module } from '@nestjs/common';
import { ProfileController } from './profile.controller';
import { ProfileService } from './profile.service';
import { PrismaService } from 'src/prisma/prisma.service';
import { AppMailerService } from 'src/mailer/mailer.service';

@Module({
    controllers: [ProfileController],
    providers: [ProfileService, PrismaService, AppMailerService]
})
export class ProfileModule {}
