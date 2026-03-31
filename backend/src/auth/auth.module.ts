import { Module } from '@nestjs/common';
import { AuthService } from './auth.service';
import { AuthController } from './auth.controller';
import { PrismaService } from 'src/prisma/prisma.service';
import { AppMailerService } from 'src/mailer/mailer.service';
import { AuthGuard } from './auth.guard';

@Module({
    providers: [AuthService, PrismaService, AppMailerService, AuthGuard],
    controllers: [AuthController],
    exports: [AuthService, AuthGuard],
})
export class AuthModule {}
