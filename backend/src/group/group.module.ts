import { Module } from '@nestjs/common';
import { GroupController } from './group.controller';
import { GroupService } from './group.service';
import { PrismaModule } from 'src/prisma/prisma.module';
import { AuthModule } from 'src/auth/auth.module';
import { CdnModule } from 'src/cdn/cdn.module';

@Module({
    imports: [PrismaModule, AuthModule, CdnModule],
    controllers: [GroupController],
    providers: [GroupService],
})
export class GroupModule {}
