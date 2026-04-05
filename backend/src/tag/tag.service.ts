import { Injectable } from '@nestjs/common';
import { PrismaService } from 'src/prisma/prisma.service';

@Injectable()
export class TagService {
    constructor(private readonly prisma: PrismaService) {}



    async getPopularTag(): Promise<string[]> {
        const tags = await this.prisma.tag.findMany({
            select: { name: true },
            orderBy: { nbUses: 'desc' },
            take: 10
        });

        return tags.map(t => t.name);
    }



    async suggestTag(tag: string): Promise<string[]> {
        const tags = await this.prisma.tag.findMany({
            where: { name: { contains: tag } },
            select: { name: true },
            orderBy: { nbUses: 'desc' },
            take: 10
        });

        return tags.map(t => t.name);
    }
}
