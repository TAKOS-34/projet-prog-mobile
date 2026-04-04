import { Module, forwardRef } from '@nestjs/common';
import { CdnController } from './cdn.controller';
import { CdnService } from './cdn.service';
import { GroupModule } from 'src/group/group.module';
import { PrismaModule } from 'src/prisma/prisma.module';
import { AuthModule } from 'src/auth/auth.module';

@Module({
    imports: [PrismaModule, AuthModule, forwardRef(() => GroupModule)],
    controllers: [CdnController],
    providers: [CdnService],
    exports: [CdnService],
})
export class CdnModule {}
