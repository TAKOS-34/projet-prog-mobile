import { Module } from '@nestjs/common';
import { AuthService } from './auth.service';
import { AuthController } from './auth.controller';
import { PrismaService } from 'src/prisma/prisma.service';
import { AppMailerService } from 'src/mailer/mailer.service';

@Module({
    providers: [AuthService, PrismaService, AppMailerService],
    controllers: [AuthController]
})
export class AuthModule {}
