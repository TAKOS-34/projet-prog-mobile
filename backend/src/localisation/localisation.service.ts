import { Injectable } from '@nestjs/common';
import { PrismaService } from 'src/prisma/prisma.service';
import { localisation } from './dto/localisation.dto';
import { UserSession } from 'src/utils/dto/userSession.dto';

@Injectable()
export class LocalisationService {
    constructor(private readonly prisma: PrismaService) {}



    async getLocalisation(localisation: string, user: UserSession): Promise<localisation> {
        const [targetLocalisation, popularLocalisations] = await this.prisma.$transaction([
            this.prisma.localisation.findUniqueOrThrow({
                where: { name: localisation },
                select: {
                    id: true,
                    name: true,
                    nbUses: true,
                    followers: { where: { followerId: user.id }, select: { followerId: true } }
                }
            }),

            this.prisma.localisation.findMany({ select: { id: true }, orderBy: { nbUses: 'desc' }, take: 5 })
        ]);

        return {
            id: targetLocalisation.id,
            name: targetLocalisation.name,
            nbUses: targetLocalisation.nbUses,
            isPopular: popularLocalisations.some(l => l.id === targetLocalisation.id),
            isFollowing: targetLocalisation.followers.length > 0
        };
    }
}
