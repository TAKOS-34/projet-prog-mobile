import { Module } from '@nestjs/common';
import { AppMailerService } from './mailer.service';

@Module({
    providers: [AppMailerService]
})
export class AppMailerModule {}
