import { Module, forwardRef } from '@nestjs/common';
import { GroupController } from './group.controller';
import { GroupService } from './group.service';
import { PrismaModule } from 'src/prisma/prisma.module';
import { AuthModule } from 'src/auth/auth.module';
import { CdnModule } from 'src/cdn/cdn.module';
import { GroupProtectGuard } from './group.protect.guard';

@Module({
    imports: [PrismaModule, AuthModule, forwardRef(() => CdnModule)],
    controllers: [GroupController],
    providers: [GroupService, GroupProtectGuard],
    exports: [GroupProtectGuard]
})
export class GroupModule {}
