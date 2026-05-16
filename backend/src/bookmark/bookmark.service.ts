import { Injectable, UnauthorizedException } from '@nestjs/common';
import { CdnService } from 'src/cdn/cdn.service';
import { PostsInfos } from 'src/post/dto/postInfos.dto';
import { PrismaService } from 'src/prisma/prisma.service';
import { TripInfos } from 'src/trip/dto/tripInfos.dto';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { UserSession } from 'src/utils/dto/userSession.dto';
import { getNextCursor } from 'src/utils/paginator/paginate.util';

@Injectable()
export class BookmarkService {
    constructor(
        private readonly prisma: PrismaService,
        private readonly cdn: CdnService
    ) {}



    async addPostBookmark(postId: string, user: UserSession): Promise<ResponseMessage> {
        const post = await this.prisma.post.findUniqueOrThrow({ where: { id: postId } });

        if (post.groupId && !await this.prisma.member.findUnique({
            where: { groupId_userId: { groupId: post.groupId, userId: user.id } },
            select: { groupId: true }
        })) {
            throw new UnauthorizedException(`You're not in the group`);
        }

        await this.prisma.postBookmark.create({ data: {
            userId: user.id, postId: postId
        }});

        return { status: true, message: 'Bookmark added' };
    }



    async deletePostBookmark(postId: string, user: UserSession): Promise<ResponseMessage> {
        await this.prisma.postBookmark.delete({ where: {
            postId_userId: {
                postId: postId,
                userId: user.id
            }
        }});

        return { status: true, message: 'Bookmark deleted' };
    }



    async getPostBookmark(user: UserSession, limit: number, cursor?: string): Promise<PostsInfos> {
        const bookmarks = await this.prisma.postBookmark.findMany({ where: { userId: user.id } });

        const posts = await this.prisma.post.findMany({
            where: { id: { in: bookmarks.map(b => b.postId) } },
            take: limit,
            skip: cursor ? 1 : 0,
            ...(cursor ? { cursor: { id: cursor } } : {}),
            orderBy: [{ creationDate: 'desc' }, { id: 'desc' }],
            include: {
                Localisation: { select: { id: true, name: true, long: true, lat: true } },
                Group: { select: { id: true, name: true, avatar: true, members: { where: { userId: user.id }, select: { userId: true } } } },
                User: { select: { id: true, username: true, avatar: true } },
                postTags: { select: { tag: { select: { name: true } } } },
                likes: { where: { userId: user.id }, select: { id: true } },
                bookmarks: { where: { userId: user.id }, select: { userId: true } }
            }
        });

        return {
            posts: posts.map(post => ({
                id: post.id,
                image: this.cdn.getPostUrl(post.id, post.imageExt),
                creationDate: post.creationDate,
                isEdited: post.isEdited,
                updatedAt: post.updatedAt ?? undefined,
                title: post.title,
                type: post.type,
                localisation: post.Localisation.name,
                long: Number(post.Localisation.long),
                lat: Number(post.Localisation.lat),
                description: post.description ?? undefined,
                audio: post.audio ? this.cdn.getAudioUrl(post.audio) : undefined,
                audioDuration: post.audioDuration ?? undefined,
                nbLikes: post.nbLikes,
                nbComments: post.nbComments,
                userId: post.User.id,
                username: post.User.username,
                avatar: this.cdn.getAvatarUrl(post.User.avatar),
                groupId: post.Group?.id ?? undefined,
                groupName: post.Group?.name ?? undefined,
                groupAvatar: post.Group?.avatar ? this.cdn.getGroupAvatarUrl(post.Group.avatar) : undefined,
                isMember: post.Group ? post.Group.members?.length > 0 : undefined,
                tags: post.postTags.map(pt => pt.tag.name),
                isLiked: post.likes?.length > 0,
                isYours: user.id === post.User.id,
                isBookmarked: post.bookmarks?.length > 0
            })),
            nextCursor: getNextCursor(posts, limit)
        };
    }



    async addTripBookmark(tripId: number, user: UserSession): Promise<ResponseMessage> {
        await this.prisma.tripBookmark.create({ data: {
            tripId,
            userId: user.id
        }});

        return { status: true, message: 'Bookmark added' };
    }



    async deleteTripBookmark(tripId: number, user: UserSession): Promise<ResponseMessage> {
        await this.prisma.tripBookmark.delete({ where: {
            tripId_userId: { tripId, userId: user.id }
        }});

        return { status: true, message: 'Bookmark deleted' };
    }



    async getTripBookmark(user: UserSession, limit: number, cursor?: number): Promise<TripInfos> {
        const bookmarks = await this.prisma.tripBookmark.findMany({ where: { userId: user.id } });

        const trips = await this.prisma.trip.findMany({
            where: { id: { in: bookmarks.map(b => b.tripId) } },
            take: limit + 1,
            skip: cursor ? 1 : 0,
            ...(cursor ? { cursor: { id: cursor } } : {}),
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
                totalDistance: trip.totalDistance ?? undefined,
                weather: trip.weather,
                difficulty: trip.difficulty ?? undefined,
                totalAscent: trip.totalAscent ?? undefined,
                nbLikes: trip.nbLikes ?? 0,
                nbBookmarks: trip.nbBookmarks ?? 0,
                isLiked: trip.likes?.length > 0,
                isBookmarked: trip.bookmarks?.length > 0,
                isYours: user.id === trip.User.id,
                userId: trip.User.id,
                username: trip.User.username,
                avatar: this.cdn.getAvatarUrl(trip.User.avatar),
                steps: trip.tripSteps.map(step => {
                    const { Localisation, ...postData } = step.Post;
                    return {
                        post: { ...postData, image: this.cdn.getPostUrl(postData.id, postData.imageExt) },
                        localisation: Localisation,
                        travelTimeFromPrevious: step.travelTimeFromPrevious,
                        travelDistanceFromPrevious: step.travelDistanceFromPrevious ?? undefined,
                        isTravelTimeFromPreviousTrusted: step.isTravelTimeFromPreviousTrusted,
                        visitDuration: step.visitDuration,
                        isVisitDurationTrusted: step.isVisitDurationTrusted
                    };
                })
            })),
            nextCursor: getNextCursor(trips, limit)
        };
    }
}
