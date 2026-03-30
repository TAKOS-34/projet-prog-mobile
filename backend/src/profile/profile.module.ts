import { Module } from '@nestjs/common';
import { ProfileController } from './profile.controller';
import { ProfileService } from './profile.service';
import { PrismaService } from 'src/prisma/prisma.service';
import { AppMailerService } from 'src/mailer/mailer.service';
import { CdnService } from 'src/cdn/cdn.service';

@Module({
    controllers: [ProfileController],
    providers: [ProfileService, PrismaService, AppMailerService, CdnService]
})
export class ProfileModule {}
