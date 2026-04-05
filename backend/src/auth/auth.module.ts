import { Module } from '@nestjs/common';
import { AuthService } from './auth.service';
import { AuthController } from './auth.controller';
import { AuthGuard } from './auth.guard';
import { PrismaModule } from 'src/prisma/prisma.module';
import { AppMailerModule } from 'src/mailer/mailer.module';
import { AuthOptionalGuard } from './auth.optionnal.guard';

@Module({
    imports: [PrismaModule, AppMailerModule],
    providers: [AuthService, AuthGuard, AuthOptionalGuard],
    controllers: [AuthController],
    exports: [AuthService, AuthGuard, AuthOptionalGuard],
})
export class AuthModule {}
