import { Module } from '@nestjs/common';
import { ProfileController } from './profile.controller';
import { ProfileService } from './profile.service';
import { PrismaModule } from 'src/prisma/prisma.module';
import { AppMailerModule } from 'src/mailer/mailer.module';
import { CdnModule } from 'src/cdn/cdn.module';
import { AuthModule } from 'src/auth/auth.module';

@Module({
    imports: [PrismaModule, AppMailerModule, CdnModule, AuthModule],
    controllers: [ProfileController],
    providers: [ProfileService],
})
export class ProfileModule {}
