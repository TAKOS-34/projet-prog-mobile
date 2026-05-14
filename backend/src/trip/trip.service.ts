import { Injectable } from "@nestjs/common";
import { TripStatus } from "@prisma/client";
import { PrismaService } from "src/prisma/prisma.service";
import { ResponseMessage } from "src/utils/dto/responseMessage.dto";
import { UserSession } from "src/utils/dto/userSession.dto";
import { TripDto, TripInfos } from "./dto/tripInfos.dto";
import { getNextCursor } from "src/utils/paginator/paginate.util";
import { CdnService } from "src/cdn/cdn.service";
import { LocalisationService } from "src/localisation/localisation.service";
import { ConfirmTripDto } from "./dto/confirmTrip.dto";


@Injectable()
export class TripService {
    constructor(
        private readonly prisma: PrismaService,
        private readonly cdn: CdnService,
        private readonly loc: LocalisationService
    ) {}



    async confirmTrip(tripId: number, trip: ConfirmTripDto, user: UserSession): Promise<ResponseMessage> {
        const cleanLocalisation = trip.localisation.toLowerCase().trim();
        const { long, lat } = await this.loc.getCoordinates(cleanLocalisation);

        const localisation = await this.prisma.localisation.upsert({
            where: { name: cleanLocalisation }, update: {},
            create: { name: cleanLocalisation, long: long, lat: lat }
        });

        await this.prisma.trip.update({
            where: { id: tripId, userId: user.id },
            data: { status: TripStatus.VALIDATED, expiresAt: null, startLocalisationId: localisation.id }
        });

        return { status: true, message: 'Trip saved' };
    }



    async getMyTrips(user: UserSession, limit: number, cursor?: number): Promise<TripInfos> {
        const trips = await this.prisma.trip.findMany({
            take: limit + 1,
            skip: cursor ? 1 : 0,
            ...(cursor ? { cursor: { id: cursor } } : {}),
            where: { userId: user.id, status: TripStatus.VALIDATED },
            orderBy: { creationDate: 'desc' },
            include: {
                tripSteps: {
                    orderBy: { stepNumber: 'asc' },
                    include: { Post: { include: { Localisation: true } } }
                },
                likes: { where: { userId: user.id }, select: { tripId: true } },
                bookmarks: { where: { userId: user.id }, select: { userId: true } },
                User: { select: { id: true, username: true, avatar: true } },
                StartLocalisation: { select: { id: true, name: true, long: true, lat: true, nbUses: true } }
            }
        });

        return {
            trips: trips.map(trip => ({
                id: trip.id,
                startLocalisation: trip.StartLocalisation ?? undefined,
                creationDate: trip.creationDate,
                category: trip.category,
                startingTime: trip.startingTime,
                transportMode: trip.transportMode,
                totalDuration: trip.duration,
                totalCost: trip.budget,
                totalStep: trip.tripSteps.length,
                weather: trip.weather,
                difficulty: trip.difficulty ?? undefined,
                nbLikes: trip.nbLikes ?? 0,
                nbBookmarks: trip.nbBookmarks ?? 0,
                isLiked: trip.likes?.length > 0,
                isBookmarked: trip.bookmarks?.length > 0,
                userId: trip.User.id,
                username: trip.User.username,
                avatar: this.cdn.getAvatarUrl(trip.User.avatar),
                steps: trip.tripSteps.map(step => {
                    const { Localisation, ...postData } = step.Post;
                    return {
                        post: { ...postData, image: this.cdn.getPostUrl(postData.id, postData.imageExt) },
                        localisation: Localisation,
                        travelTimeFromPrevious: step.travelTimeFromPrevious,
                        isTravelTimeFromPreviousTrusted: step.isTravelTimeFromPreviousTrusted,
                        visitDuration: step.visitDuration,
                        isVisitDurationTrusted: step.isVisitDurationTrusted
                    };
                })
            })),
            nextCursor: getNextCursor(trips, limit)
        }
    }



    async getTrips(limit: number, cursor?: number, userId?: number): Promise<TripInfos> {
        const trips = await this.prisma.trip.findMany({
            take: limit + 1,
            skip: cursor ? 1 : 0,
            ...(cursor ? { cursor: { id: cursor } } : {}),
            where: { status: TripStatus.VALIDATED },
            orderBy: { creationDate: 'desc' },
            include: {
                tripSteps: {
                    orderBy: { stepNumber: 'asc' },
                    include: { Post: { include: { Localisation: true } } }
                },
                likes: userId ? { where: { userId }, select: { tripId: true } } : false,
                bookmarks: userId ? { where: { userId }, select: { userId: true } } : false,
                User: { select: { id: true, username: true, avatar: true } },
                StartLocalisation: { select: { id: true, name: true, long: true, lat: true, nbUses: true } }
            }
        });

        return {
            trips: trips.map(trip => ({
                id: trip.id,
                startLocalisation: trip.StartLocalisation ?? undefined,
                creationDate: trip.creationDate,
                category: trip.category,
                startingTime: trip.startingTime,
                transportMode: trip.transportMode,
                totalDuration: trip.duration,
                totalCost: trip.budget,
                totalStep: trip.tripSteps.length,
                weather: trip.weather,
                difficulty: trip.difficulty ?? undefined,
                nbLikes: trip.nbLikes ?? 0,
                nbBookmarks: trip.nbBookmarks ?? 0,
                isLiked: trip.likes?.length > 0,
                isBookmarked: trip.bookmarks?.length > 0,
                userId: trip.User.id,
                username: trip.User.username,
                avatar: this.cdn.getAvatarUrl(trip.User.avatar),
                steps: trip.tripSteps.map(step => {
                    const { Localisation, ...postData } = step.Post;
                    return {
                        post: { ...postData, image: this.cdn.getPostUrl(postData.id, postData.imageExt) },
                        localisation: Localisation,
                        travelTimeFromPrevious: step.travelTimeFromPrevious,
                        isTravelTimeFromPreviousTrusted: step.isTravelTimeFromPreviousTrusted,
                        visitDuration: step.visitDuration,
                        isVisitDurationTrusted: step.isVisitDurationTrusted
                    };
                })
            })),
            nextCursor: getNextCursor(trips, limit)
        }
    }



    async getTrip(tripId: number, userId?: number): Promise<TripDto> {
        const trip = await this.prisma.trip.findUniqueOrThrow({
            where: { id: tripId, status: TripStatus.VALIDATED },
            include: {
                tripSteps: {
                    orderBy: { stepNumber: 'asc' },
                    include: { Post: { include: { Localisation: true } } }
                },
                likes: userId ? { where: { userId }, select: { tripId: true } } : false,
                bookmarks: userId ? { where: { userId }, select: { userId: true } } : false,
                User: { select: { id: true, username: true, avatar: true } },
                StartLocalisation: { select: { id: true, name: true, long: true, lat: true, nbUses: true } }
            }
        });

        return {
            id: trip.id,
            startLocalisation: trip.StartLocalisation ?? undefined,
            creationDate: trip.creationDate,
            category: trip.category,
            startingTime: trip.startingTime,
            transportMode: trip.transportMode,
            totalDuration: trip.duration,
            totalCost: trip.budget,
            totalStep: trip.tripSteps.length,
            weather: trip.weather,
            difficulty: trip.difficulty ?? undefined,
            nbLikes: trip.nbLikes ?? 0,
            nbBookmarks: trip.nbBookmarks ?? 0,
            isLiked: trip.likes?.length > 0,
            isBookmarked: trip.bookmarks?.length > 0,
            userId: trip.User.id,
            username: trip.User.username,
            avatar: this.cdn.getAvatarUrl(trip.User.avatar),
            steps: trip.tripSteps.map(step => {
                const { Localisation, ...postData } = step.Post;
                return {
                    post: { ...postData, image: this.cdn.getPostUrl(postData.id, postData.imageExt) },
                    localisation: Localisation,
                    travelTimeFromPrevious: step.travelTimeFromPrevious,
                    isTravelTimeFromPreviousTrusted: step.isTravelTimeFromPreviousTrusted,
                    visitDuration: step.visitDuration,
                    isVisitDurationTrusted: step.isVisitDurationTrusted
                };
            })
        };
    }
}