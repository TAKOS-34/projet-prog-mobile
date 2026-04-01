import { Module } from '@nestjs/common';
import { AuthService } from './auth.service';
import { AuthController } from './auth.controller';
import { AuthGuard } from './auth.guard';
import { PrismaModule } from 'src/prisma/prisma.module';
import { AppMailerModule } from 'src/mailer/mailer.module';

@Module({
    imports: [PrismaModule, AppMailerModule],
    providers: [AuthService, AuthGuard],
    controllers: [AuthController],
    exports: [AuthService, AuthGuard],
})
export class AuthModule {}
