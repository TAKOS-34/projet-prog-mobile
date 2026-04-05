import { Module } from '@nestjs/common';
import { TagController } from './tag.controller';
import { TagService } from './tag.service';
import { AuthModule } from 'src/auth/auth.module';
import { PrismaModule } from 'src/prisma/prisma.module';

@Module({
    imports: [AuthModule, PrismaModule],
    controllers: [TagController],
    providers: [TagService]
})
export class TagModule {}
