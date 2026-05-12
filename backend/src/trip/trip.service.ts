import { Injectable } from '@nestjs/common';
import { Localisation, Post, PostType, TripCategory, TripStartingTime } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';
import { SuggestTripDto } from './dto/suggestTrip.dto';
import { LocalisationService } from 'src/localisation/localisation.service';
import { TripSuggestInfos, ScoredPost, Weather, WeatherCode, TripSuggestResponse } from './dto/tripInfos.dto';
import { UserSession } from 'src/utils/dto/userSession.dto';

interface Point {
    long: number;
    lat: number;
}



@Injectable()
export class TripService {
    private readonly OUTDOOR_TYPES: PostType[] = [PostType.PANORAMA, PostType.NATURAL_AREA, PostType.COASTAL_WATER];

    constructor(
        private prisma: PrismaService,
        private loc: LocalisationService
    ) {}



    async suggestTrips(suggestTrip: SuggestTripDto, user: UserSession): Promise<TripSuggestResponse> {
        const locCoords: Point = await this.loc.getCoordinates(suggestTrip.localisation);

        const locIds: number[] | null = await this.loc.getNearbyLocalisationIds(suggestTrip.localisation, 50);
        const candidates: (Post & { Localisation: Localisation })[] = await this.fetchCandidates(locIds);

        const standardizedCandidates: (Post & { Localisation: Localisation })[] = this.applyFallbacks(candidates);

        const weather: Weather = await this.weatherAdaptor(locCoords);

        const normalCandidates: ScoredPost[] = this.scoreCandidates(standardizedCandidates, suggestTrip, TripCategory.NORMAL, weather);
        const businessCandidates: ScoredPost[] = this.scoreCandidates(standardizedCandidates, suggestTrip, TripCategory.BUISNESS, weather);

        const normalTrip: TripSuggestInfos = this.buildPath(locCoords, suggestTrip, normalCandidates, TripCategory.NORMAL);
        const businessTrip: TripSuggestInfos = this.buildPath(locCoords, suggestTrip, businessCandidates, TripCategory.BUISNESS);

        const [normalTripData, businessTripData] = await Promise.all([
            this.prisma.trip.create({
                data: this.tripBuilder(normalTrip, TripCategory.NORMAL, suggestTrip.startingTime, user.id),
                select: { id: true }
            }),
            this.prisma.trip.create({
                data: this.tripBuilder(businessTrip, TripCategory.BUISNESS, suggestTrip.startingTime, user.id),
                select: { id: true }
            })
        ]);

        return { trips: [ { ...normalTrip, id: normalTripData.id }, { ...businessTrip, id: businessTripData.id } ], weather };
    }



    private async fetchCandidates(locIds: number[] | null): Promise<(Post & { Localisation: Localisation })[]> {
        if (!locIds || locIds.length === 0) return [];
            return this.prisma.post.findMany({
                where: { localisationId: { in: locIds } },
                include: { Localisation: true }
        });
    }



    private applyFallbacks(posts: (Post & { Localisation: Localisation })[]): (Post & { Localisation: Localisation })[] {
        return posts.map(post => {
            let minDuration = post.minDuration;
            let minPrice = post.minPrice;

            if (!minDuration) {
                switch (post.type) {
                    case PostType.PANORAMA: minDuration = 30; break;
                    case PostType.HISTORIC_SITE:
                    case PostType.ART_CULTURE: minDuration = 120; break;
                    case PostType.GASTRONOMY: minDuration = 90; break;
                    case PostType.UNIQUE_STAY: minDuration = 720; break;
                    default: minDuration = 60;
                }
            }

            if (minPrice === null || minPrice === undefined) {
                switch (post.type) {
                    case PostType.GASTRONOMY: minPrice = 2000; break;
                    case PostType.UNIQUE_STAY: minPrice = 10000; break;
                    case PostType.NIGHTLIFE: minPrice = 1500; break;
                    default: minPrice = 0;
                }
            }

            return { ...post, minDuration, minPrice };
        });
    }



    private scoreCandidates(posts: (Post & { Localisation: Localisation })[], request: SuggestTripDto, category: TripCategory, weather: Weather): ScoredPost[] {
        return posts.reduce((acc, post) => {
            let score = 10;

            const isOutdoor = this.OUTDOOR_TYPES.includes(post.type);
            if (isOutdoor && [WeatherCode.RAIN, WeatherCode.STORM, WeatherCode.SNOW].includes(weather.code)) {
                return acc;
            }

            if (request.preferredTypes?.includes(post.type)) {
                score += 50;
            }

            const time = request.startingTime;

            switch (post.type) {
                case PostType.GASTRONOMY:
                    if (time === TripStartingTime.MIDDAY) score += 40;
                    if (time === TripStartingTime.EVENING) score += 50;
                    break;
                case PostType.NIGHTLIFE:
                    if (time === TripStartingTime.EVENING) score += 70;
                    if (time === TripStartingTime.AFTERNOON) score -= 20;
                    break;
                case PostType.PANORAMA:
                    if (time === TripStartingTime.MORNING || time === TripStartingTime.EVENING) score += 30;
                    break;
                case PostType.ART_CULTURE:
                case PostType.HISTORIC_SITE:
                    if (time === TripStartingTime.MORNING || time === TripStartingTime.AFTERNOON) score += 20;
                    break;
            }

            if (category === TripCategory.BUISNESS && (post.type === PostType.UNIQUE_STAY || post.type === PostType.GASTRONOMY)) {
                score += 30;
            }

            if (category === TripCategory.NORMAL && post.minPrice === 0) {
                score += 20;
            }

            acc.push({ ...post, score });
            return acc;
        }, [] as ScoredPost[]);
    }



    private buildPath(start: Point, request: SuggestTripDto, candidates: ScoredPost[], category: TripCategory): TripSuggestInfos {
        const selectedPosts: ScoredPost[] = [];
        let currentDuration = 0;
        let currentCost = 0;
        let currentLocation = start;

        let unvisited = [...candidates];

        while (unvisited.length > 0) {
            const validCandidates: { post: ScoredPost; ratio: number; travelTime: number }[] = [];

            for (const post of unvisited) {
                const postDuration = post.minDuration!;
                const postCost = post.minPrice!;
                const postCoords = { lat: Number(post.Localisation.lat), long: Number(post.Localisation.long) };

                const travelTime = this.calculateTravelTime(currentLocation, postCoords);
                const timeToReturnStart = this.calculateTravelTime(postCoords, start);

                if (currentDuration + travelTime + postDuration + timeToReturnStart <= request.maxDuration && currentCost + postCost <= request.maxBudget) {
                    const dynamicScore = this.getDynamicScore(post, currentDuration + travelTime, request.startingTime, category, request.preferredTypes);
                    const ratio = dynamicScore / (travelTime + postDuration);

                    validCandidates.push({ post, ratio, travelTime });
                }
            }

            if (validCandidates.length === 0) break;

            validCandidates.sort((a, b) => b.ratio - a.ratio);

            const RANDOM_POOL_SIZE = 3;
            const poolSize = Math.min(RANDOM_POOL_SIZE, validCandidates.length);
            const randomIndex = Math.floor(Math.random() * poolSize);
            const selected = validCandidates[randomIndex];

            const bestNextPost = selected.post;
            const travelTimeToBest = selected.travelTime;

            selectedPosts.push(bestNextPost);
            currentDuration += travelTimeToBest + bestNextPost.minDuration!;
            currentCost += bestNextPost.minPrice!;
            currentLocation = { 
                lat: Number(bestNextPost.Localisation.lat), 
                long: Number(bestNextPost.Localisation.long) 
            };

            unvisited = unvisited.filter(p => p.id !== bestNextPost.id);
        }

        return {
            steps: selectedPosts,
            totalDuration: Math.floor(currentDuration + this.calculateTravelTime(currentLocation, start)),
            totalCost: currentCost,
            totalStep: selectedPosts.length
        };
    }



    private getDynamicScore(post: Post, currentElapsedMinutes: number, startingTime: TripStartingTime, category: TripCategory, preferredTypes?: PostType[]): number {
        let score = 10;

        const simulatedHour = this.getSimulatedHour(startingTime, currentElapsedMinutes);

        if (preferredTypes?.includes(post.type)) score += 50;

        switch (post.type) {
            case PostType.GASTRONOMY:
                if (simulatedHour >= 12 && simulatedHour <= 14) score += 100;
                if (simulatedHour >= 19 && simulatedHour <= 21) score += 100;
                break;
                
            case PostType.NIGHTLIFE:
                if (simulatedHour >= 22) score += 120;
                else if (simulatedHour >= 18) score += 60;
                else score -= 50;
                break;

            case PostType.PANORAMA:
                if (simulatedHour === 8 || simulatedHour === 18) score += 40;
                break;
        }

        if (category === TripCategory.BUISNESS && (post.type === PostType.UNIQUE_STAY || post.type === PostType.GASTRONOMY)) score += 30;
        if (category === TripCategory.NORMAL && post.minPrice === 0) score += 20;

        return score;
    }



    private getSimulatedHour(startingTime: string, elapsedMinutes: number) {
        let baseHour: number;

        switch (startingTime) {
            case TripStartingTime.MORNING:   baseHour = 8; break;
            case TripStartingTime.MIDDAY:    baseHour = 12; break;
            case TripStartingTime.AFTERNOON: baseHour = 14; break;
            case TripStartingTime.EVENING:   baseHour = 19; break;
            default: baseHour = 9;
        }

        return (baseHour + elapsedMinutes / 60) % 24;
    }



    private getHaversineDistance(p1: Point, p2: Point): number {
        const R = 6371;
        const dLat = (p2.lat - p1.lat) * Math.PI / 180;
        const dLon = (p2.long - p1.long) * Math.PI / 180;
        const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(p1.lat * Math.PI / 180) * Math.cos(p2.lat * Math.PI / 180) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }



    private calculateTravelTime(from: Point, to: Point): number {
        const distanceKm = this.getHaversineDistance(from, to);
        if (distanceKm < 3) {
            return (distanceKm / 5) * 60;
        } else {
            return (distanceKm / 50) * 60 + 10;
        }
    }



    private async weatherAdaptor(loc: Point): Promise<Weather> {
        try {
            const response = await fetch(`https://api.open-meteo.com/v1/forecast?latitude=${loc.lat}&longitude=${loc.long}&current=temperature_2m,weather_code`);
            const data = await response.json();
            return { code: this.getWeatherCode(data?.current?.weather_code), temperature: data?.current?.temperature_2m };
        } catch (e) {
            return { code: WeatherCode.CLEAR, temperature: 15 };
        }
    }



    private getWeatherCode(code: number): WeatherCode {
        if (code <= 1) return WeatherCode.CLEAR;
        if (code <= 3 || code === 45 || code === 48) return WeatherCode.CLOUDY;
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return WeatherCode.RAIN;
        if ((code >= 71 && code <= 77) || (code >= 85 && code <= 86)) return WeatherCode.SNOW;
        if (code >= 95) return WeatherCode.STORM;
        return WeatherCode.CLEAR;
    }



    private tripBuilder(trip: any, category: TripCategory, startingTime: TripStartingTime, userId: number) {
        return {
            budget: trip.totalCost,
            duration: trip.totalDuration,
            category: category,
            startingTime: startingTime,
            userId: userId,
            tripSteps: {
                create: trip.steps.map((step: any, index: number) => ({
                    postId: step.id,
                    stepNumber: index + 1
                }))
            }
        };
    }
}