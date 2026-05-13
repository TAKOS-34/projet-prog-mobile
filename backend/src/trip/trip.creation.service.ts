import { Injectable } from '@nestjs/common';
import { Localisation, Post, PostType, TripCategory, TripStartingTime, TripTransportMode, WeatherCode } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';
import { SuggestTripDto } from './dto/suggestTrip.dto';
import { LocalisationService } from 'src/localisation/localisation.service';
import { TripSuggestInfos, ScoredPost, Weather, TripSuggest, TripStepDetail } from './dto/tripInfos.dto';
import { UserSession } from 'src/utils/dto/userSession.dto';
import { CdnService } from 'src/cdn/cdn.service';

interface Point {
    long: number;
    lat: number;
}
interface Candidate {
    post: ScoredPost,
    ratio: number,
    travelTime: number
}



@Injectable()
export class TripCreationService {
    private readonly OUTDOOR_TYPES: PostType[] = [PostType.PANORAMA, PostType.NATURAL_AREA, PostType.COASTAL_WATER];
    private readonly ORS_API_KEY: string = process.env.ORS_API_KEY ?? '';

    constructor(
        private readonly prisma: PrismaService,
        private readonly loc: LocalisationService,
        private readonly cdn: CdnService
    ) {}



    async suggestTrips(suggestTrip: SuggestTripDto, user: UserSession): Promise<TripSuggest> {
        const locCoords: Point = await this.loc.getCoordinates(suggestTrip.localisation);

        const locIds: number[] | null = await this.loc.getNearbyLocalisationIds(suggestTrip.localisation, 50);
        const candidates: (Post & { Localisation: Localisation })[] = await this.fetchCandidates(locIds);

        const standardizedCandidates: (Post & { Localisation: Localisation })[] = this.applyFallbacks(candidates);

        const weather: Weather = await this.weatherAdaptor(locCoords);

        const normalCandidates: ScoredPost[] = this.scoreCandidates(standardizedCandidates, suggestTrip, TripCategory.NORMAL, weather.code);
        const businessCandidates: ScoredPost[] = this.scoreCandidates(standardizedCandidates, suggestTrip, TripCategory.BUISNESS, weather.code);

        let normalTrip: TripSuggestInfos = this.buildPath(locCoords, suggestTrip, normalCandidates, TripCategory.NORMAL, weather.code);
        let businessTrip: TripSuggestInfos = this.buildPath(locCoords, suggestTrip, businessCandidates, TripCategory.BUISNESS, weather.code);

        [normalTrip, businessTrip] = await Promise.all([
            this.refineWithDirectionsAPI(locCoords, normalTrip, suggestTrip.transportMode),
            this.refineWithDirectionsAPI(locCoords, businessTrip, suggestTrip.transportMode)
        ]);

        const [normalTripData, businessTripData] = await Promise.all([
            this.prisma.trip.create({
                data: this.tripBuilder(normalTrip, TripCategory.NORMAL, suggestTrip.transportMode, suggestTrip.startingTime, weather.code, user.id),
                select: { id: true }
            }),
            this.prisma.trip.create({
                data: this.tripBuilder(businessTrip, TripCategory.BUISNESS, suggestTrip.transportMode, suggestTrip.startingTime, weather.code, user.id),
                select: { id: true }
            })
        ]);

        return { trips: [ { ...normalTrip, id: normalTripData.id }, { ...businessTrip, id: businessTripData.id } ], weather };
    }



    private async fetchCandidates(locIds: number[] | null): Promise<(Post & { Localisation: Localisation })[]> {
        if (!locIds || locIds.length === 0) return [];
            return this.prisma.post.findMany({
                where: { localisationId: { in: locIds }, groupId: null },
                include: { Localisation: true }
        });
    }



    private applyFallbacks(posts: (Post & { Localisation: Localisation })[]): (Post & { Localisation: Localisation })[] {
        return posts.map(post => {
            let minDuration = post.minDuration;
            let isDurationTrusted = true;
            let minPrice = post.minPrice;

            if (!minDuration) {
                isDurationTrusted = false;

                switch (post.type) {
                    case PostType.PANORAMA: minDuration = 30; break;
                    case PostType.HISTORIC_SITE:
                    case PostType.ART_CULTURE: minDuration = 120; break;
                    case PostType.GASTRONOMY: minDuration = 60; break;
                    case PostType.UNIQUE_STAY: minDuration = 720; break;
                    default: minDuration = 60;
                }
            }

            if (minPrice === null) {
                switch (post.type) {
                    case PostType.GASTRONOMY: minPrice = 20; break;
                    case PostType.UNIQUE_STAY: minPrice = 100; break;
                    case PostType.NIGHTLIFE: minPrice = 15; break;
                    default: minPrice = 0;
                }
            }

            return { ...post, minDuration, minPrice, isDurationTrusted };
        });
    }



    private scoreCandidates(posts: (Post & { Localisation: Localisation })[], suggestTrip: SuggestTripDto, category: TripCategory, weather: WeatherCode): ScoredPost[] {
        return posts.reduce((acc, post) => {
            let score = 10;

            const isOutdoor = this.OUTDOOR_TYPES.includes(post.type);
            if (isOutdoor && ([WeatherCode.RAIN, WeatherCode.STORM, WeatherCode.SNOW] as WeatherCode[]).includes(weather)) {
                return acc;
            }

            if (suggestTrip.preferredTypes?.includes(post.type)) {
                score += 50;
            }

            const time = suggestTrip.startingTime;

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



    private buildPath(start: Point, suggestTrip: SuggestTripDto, candidates: ScoredPost[], category: TripCategory, weather: WeatherCode): TripSuggestInfos {
        const selectedPosts: TripStepDetail[] = [];
        let currentDuration = 0;
        let currentCost = 0;
        let currentLocation = start;
        let unvisited = [...candidates];
        const SAFE_MAX_DURATION = suggestTrip.maxDuration * 0.90;

        while (unvisited.length > 0) {
            const validCandidates: { post: ScoredPost; ratio: number; travelTime: number }[] = [];

            for (const post of unvisited) {
                const postDuration = post.minDuration!;
                const postCost = post.minPrice!;
                const postCoords = { lat: Number(post.Localisation.lat), long: Number(post.Localisation.long) };

                const travelTime = this.calculateTravelTime(currentLocation, postCoords, suggestTrip.transportMode);

                if (currentDuration + travelTime + postDuration <= SAFE_MAX_DURATION && currentCost + postCost <= suggestTrip.maxBudget) {
                    const dynamicScore = this.getDynamicScore(post, currentDuration + travelTime, suggestTrip.startingTime, category, suggestTrip.preferredTypes);
                    const ratio = dynamicScore / (travelTime + postDuration);

                    validCandidates.push({ post, ratio, travelTime });
                }
            }

            if (validCandidates.length === 0) break;

            const selected: Candidate = this.selectRandomCandidate(validCandidates);
            const bestNextPost = selected.post;
            const travelTimeToBest = Math.floor(selected.travelTime);

            const { score, Localisation, isDurationTrusted, ...cleanPost } = bestNextPost as any;

            selectedPosts.push({
                post: { ...cleanPost, image: this.cdn.getPostUrl(cleanPost.id, cleanPost.imageExt) },
                localisation: Localisation,
                travelTimeFromPrevious: travelTimeToBest,
                isTravelTimeFromPreviousTrusted: false,
                visitDuration: cleanPost.minDuration!,
                isVisitDurationTrusted: isDurationTrusted ?? false
            });
            currentDuration += travelTimeToBest + cleanPost.minDuration!;
            currentCost += cleanPost.minPrice!;
            currentLocation = { lat: Number(Localisation.lat), long: Number(Localisation.long) };

            unvisited = unvisited.filter(p => p.id !== cleanPost.id);
        }

        return {
            steps: selectedPosts,
            totalDuration: Math.floor(currentDuration),
            totalCost: currentCost,
            totalStep: selectedPosts.length,
            weather
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



    private calculateTravelTime(from: Point, to: Point, transportMode: TripTransportMode): number {
        const distanceKm = this.getHaversineDistance(from, to);
        const urbanDistanceKm = distanceKm * 1.35;

        switch (transportMode) {
            case TripTransportMode.CAR: {
                const drivingTime = (urbanDistanceKm / 25) * 60;
                const parkingPenalty = urbanDistanceKm < 0.5 ? 5 : 15;
                return drivingTime + parkingPenalty;
            }

            case TripTransportMode.WALK: {
                return (urbanDistanceKm / 5) * 60
            }
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



    private selectRandomCandidate(candidates: Candidate[]): Candidate {
        candidates.sort((a, b) => b.ratio - a.ratio);

        const RANDOM_POOL_SIZE = 3;
        const poolSize = Math.min(RANDOM_POOL_SIZE, candidates.length);
        const randomIndex = Math.floor(Math.random() * poolSize);

        return candidates[randomIndex];
    }



    private async refineWithDirectionsAPI(start: Point, trip: TripSuggestInfos, mode: TripTransportMode): Promise<TripSuggestInfos> {
        if (trip.steps.length === 0) return trip;

        let profile;
        switch (mode) {
            case TripTransportMode.WALK: { profile = 'foot-walking'; break; }
            case TripTransportMode.CAR: { profile = 'driving-car'; break; }
            default: { profile = 'foot-walking'; break; }
        }

        const coordinates = [
            [start.long, start.lat],
            ...trip.steps.map(step => [step.localisation.long, step.localisation.lat])
        ];
        return trip;

        try {
            const response = await fetch(`https://api.openrouteservice.org/v2/directions/${profile}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Authorization': this.ORS_API_KEY },
                body: JSON.stringify({ coordinates, units: 'm' })
            });
            const data = await response.json();

            if (!data.routes || data.routes.length === 0) return trip;

            const segments = data.routes[0].segments;
            let totalTravelTime = 0;

            trip.steps.forEach((step, index) => {
                const travelMinutes = Math.round(segments[index].duration / 60);
                step.travelTimeFromPrevious = travelMinutes;
                step.isTravelTimeFromPreviousTrusted = true;
                totalTravelTime += travelMinutes;
            });

            const totalVisitTime = trip.steps.reduce((acc, step) => acc + step.visitDuration, 0);
            trip.totalDuration = totalTravelTime + totalVisitTime;

            return trip;
        } catch (error) {
            return trip;
        }
    }



    private tripBuilder(trip: TripSuggestInfos, category: TripCategory, transportMode: TripTransportMode, startingTime: TripStartingTime, weather: WeatherCode, userId: number) {
        return {
            budget: trip.totalCost,
            duration: trip.totalDuration,
            category: category,
            startingTime: startingTime,
            transportMode: transportMode,
            weather,
            userId: userId,
            tripSteps: {
                create: trip.steps.map((step: any, index: number) => ({
                    postId: step.post.id,
                    stepNumber: index + 1,
                    travelTimeFromPrevious: step.travelTimeFromPrevious,
                    isTravelTimeFromPreviousTrusted: step.isTravelTimeFromPreviousTrusted,
                    visitDuration: step.visitDuration,
                    isVisitDurationTrusted: step.isVisitDurationTrusted
                }))
            }
        };
    }
}