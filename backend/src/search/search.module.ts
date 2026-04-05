import { Module } from '@nestjs/common';
import { SearchController } from './search.controller';
import { SearchService } from './search.service';
import { PrismaModule } from 'src/prisma/prisma.module';
import { CdnModule } from 'src/cdn/cdn.module';
import { AuthModule } from 'src/auth/auth.module';

@Module({
    imports: [PrismaModule, CdnModule, AuthModule],
    controllers: [SearchController],
    providers: [SearchService]
})
export class SearchModule {}
