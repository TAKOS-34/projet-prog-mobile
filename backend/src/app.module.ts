import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { ThrottlerModule } from '@nestjs/throttler';
import { AuthModule } from './auth/auth.module';
import { ScheduleModule } from '@nestjs/schedule';
import { MailerModule } from '@nestjs-modules/mailer';

@Module({
    imports: [
        ConfigModule.forRoot(),
        ThrottlerModule.forRoot({
            throttlers: [
                {
                    ttl: 60000,
                    limit: 10,
                },
            ],
        }),
        ScheduleModule.forRoot(),
        AuthModule,
        MailerModule.forRoot({
            transport: {
                service: 'gmail',
                host: 'smtp.google.com',
                port: 587,
                auth: {
                    user: process.env.MAILER_USERNAME,
                    pass: process.env.MAILER_PASSWORD,
                },
            }
        }),
    ],
    controllers: [],
    providers: [AuthModule],
})
export class AppModule {}
