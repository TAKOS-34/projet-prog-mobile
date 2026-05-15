import { BadRequestException, Injectable } from '@nestjs/common';
import { PrismaService } from 'src/prisma/prisma.service';
import { localisation } from './dto/localisation.dto';
import { UserSession } from 'src/utils/dto/userSession.dto';
import NodeGeocoder from 'node-geocoder';

@Injectable()
export class LocalisationService {
    private readonly geocoder = NodeGeocoder({ provider: 'openstreetmap' });

    constructor(private readonly prisma: PrismaService) {}



    async getLocalisation(localisation: string, user: UserSession): Promise<localisation> {
        const [targetLocalisation, popularLocalisations] = await this.prisma.$transaction([
            this.prisma.localisation.findUniqueOrThrow({
                where: { name: localisation },
                select: { id: true, name: true, nbUses: true, long: true, lat: true,
                    followers: { where: { followerId: user.id }, select: { followerId: true } }
                }
            }),

            this.prisma.localisation.findMany({ select: { id: true }, orderBy: { nbUses: 'desc' }, take: 5 })
        ]);

        return {
            id: targetLocalisation.id,
            name: targetLocalisation.name,
            long: targetLocalisation.long,
            lat: targetLocalisation.lat,
            nbUses: targetLocalisation.nbUses,
            isPopular: popularLocalisations.some(l => l.id === targetLocalisation.id),
            isFollowing: targetLocalisation.followers.length > 0
        };
    }



    async getCoordinates(address: string): Promise<{ long: number, lat: number }> {
        const res = await this.geocoder.geocode(address);
        if (!res.length || !res[0].latitude || !res[0].longitude) {
            throw new BadRequestException('Localisation is not correct');
        }
        return { long: res[0].longitude, lat: res[0].latitude };
    }



    async getNearbyLocalisationIds(locName: string, dist: number): Promise<number[] | null> {
        let lat: number, long: number;

        const refLoc = await this.prisma.localisation.findFirst({
            where: { name: locName },
            select: { lat: true, long: true }
        });

        if (refLoc) {
            lat = Number(refLoc.lat);
            long = Number(refLoc.long);
        } else {
            const coords = await this.getCoordinates(locName);
            lat = coords.lat;
            long = coords.long;
        }

        const latDelta = dist / 111;
        const lngDelta = dist / (111 * Math.cos((lat * Math.PI) / 180));

        const nearby = await this.prisma.$queryRaw<{ id: number }[]>`
            SELECT id FROM "Localisation"
            WHERE lat BETWEEN ${lat - latDelta} AND ${lat + latDelta}
                AND long BETWEEN ${long - lngDelta} AND ${long + lngDelta}
                AND (6371 * acos(
                    cos(radians(${lat})) * cos(radians(lat)) *
                    cos(radians(long) - radians(${long})) +
                    sin(radians(${lat})) * sin(radians(lat))
                )) <= ${dist}
            `;

        return nearby.map(r => r.id);
    }
}
