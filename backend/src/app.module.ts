import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { ThrottlerModule } from '@nestjs/throttler';
import { AuthModule } from './auth/auth.module';
import { ScheduleModule } from '@nestjs/schedule';
import { MailerModule } from '@nestjs-modules/mailer';
import { HandlebarsAdapter } from '@nestjs-modules/mailer/adapters/handlebars.adapter';
import { join } from 'path';
import { ProfileModule } from './profile/profile.module';
import { CdnModule } from './cdn/cdn.module';
import { PostModule } from './post/post.module';
import { GroupModule } from './group/group.module';
import { GroupAdminModule } from './group-admin/group-admin.module';
import { LikeModule } from './like/like.module';
import { CommentModule } from './comment/comment.module';
import { TagModule } from './tag/tag.module';
import { SearchModule } from './search/search.module';
import { NotificationModule } from './notification/notification.module';

@Module({
    imports: [
        ConfigModule.forRoot(),
        ThrottlerModule.forRoot({
            throttlers: [
                {
                    ttl: 60000,
                    limit: 10,
                }
            ]
        }),
        ScheduleModule.forRoot(),
        MailerModule.forRoot({
            transport: {
                service: 'gmail',
                host: 'smtp.google.com',
                port: 587,
                auth: {
                    user: process.env.MAILER_USERNAME,
                    pass: process.env.MAILER_PASSWORD,
                }
            },
            template: {
                dir: join(__dirname, 'mailer', 'views'),
                adapter: new HandlebarsAdapter(),
                options: { strict: true }
            }
        }),
        AuthModule, ProfileModule, CdnModule, PostModule, GroupModule, GroupAdminModule, LikeModule, CommentModule, TagModule, SearchModule, NotificationModule,
    ],
    controllers: [],
    providers: []
})
export class AppModule {}
