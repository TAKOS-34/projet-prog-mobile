import { Module } from '@nestjs/common';
import { GroupAdminController } from './group-admin.controller';
import { GroupAdminService } from './group-admin.service';
import { GroupAdminGuard } from './group-admin.guard';
import { AuthModule } from 'src/auth/auth.module';
import { PrismaModule } from 'src/prisma/prisma.module';

@Module({
    imports: [AuthModule, PrismaModule],
    controllers: [GroupAdminController],
    providers: [GroupAdminService, GroupAdminGuard]
})
export class GroupAdminModule {}
