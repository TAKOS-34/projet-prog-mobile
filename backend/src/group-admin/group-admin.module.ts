import { Module } from '@nestjs/common';
import { GroupAdminController } from './group-admin.controller';
import { GroupAdminService } from './group-admin.service';
import { GroupAdminGuard } from './group-admin.guard';
import { AuthModule } from 'src/auth/auth.module';
import { PrismaModule } from 'src/prisma/prisma.module';
import { AppMailerModule } from 'src/mailer/mailer.module';
import { CdnModule } from 'src/cdn/cdn.module';

@Module({
    imports: [AuthModule, PrismaModule, AppMailerModule, CdnModule],
    controllers: [GroupAdminController],
    providers: [GroupAdminService, GroupAdminGuard]
})
export class GroupAdminModule {}
