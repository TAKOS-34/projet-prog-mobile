import { Injectable } from '@nestjs/common';
import { PrismaService } from 'src/prisma/prisma.service';
import { GroupSearch } from './dto/groupSearch.dto';
import { CdnService } from 'src/cdn/cdn.service';

@Injectable()
export class SearchService {
    constructor(
        private readonly prisma: PrismaService,
        private readonly cdn: CdnService
    ) {}



    async searchGroups(name: string, userId?: number): Promise<GroupSearch[]> {
        const groups = await this.prisma.group.findMany({
            where: { name: { contains: name } },
            select: {
                id: true,
                name: true,
                avatar: true,
                creationDate: true,
                isGroupPrivate: true,
                nbMembers: true,
                nbPosts: true,
                ...(userId ? { members: { where: { userId }, select: { userId: true }, take: 1 } } : {})
            },
            orderBy: [{ nbMembers: 'desc' }, { creationDate: 'desc' }],
            take: 20
        });

        return groups.map(g => ({
            id: g.id,
            name: g.name,
            avatar: this.cdn.getGroupAvatarUrl(g.avatar),
            creationDate: g.creationDate,
            isGroupPrivate: g.isGroupPrivate,
            isMember: userId ? g.members.length > 0 : false,
            nbMembers: g.nbMembers,
            nbPosts: g.nbPosts
        }));
    }
}
