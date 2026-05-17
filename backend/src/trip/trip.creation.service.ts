import { BadRequestException, Injectable } from '@nestjs/common';
import { Localisation, Post, PostType, TripCategory, TripStartingTime, TripTransportMode, WeatherCode } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';
import { SuggestTripDto } from './dto/suggestTrip.dto';
import { LocalisationService } from 'src/localisation/localisation.service';
import { TripSuggestInfos, Weather, TripSuggest, TripStepDetail } from './dto/tripInfos.dto';
import { UserSession } from 'src/utils/dto/userSession.dto';
import { CdnService } from 'src/cdn/cdn.service';

interface Point {
    long: number;
    lat: number;
}
interface Candidate {
    post: StandardizedPost,
    ratio: number,
    travelTime: number
}
type PreStandardizedCandidate = Post & { Localisation: Localisation };
type StandardizedPost = PreStandardizedCandidate & { effectiveDuration: number; effectivePrice: number; isDurationTrusted: boolean };



@Injectable()
export class TripCreationService {
    private readonly ORS_API_KEY: string = process.env.ORS_API_KEY ?? '';

    private readonly BASE_SCORE = 10;
    private readonly HOUR_BONUS_SCORE = 100;
    private readonly HOUR_MALUS_SCORE = 50;
    private readonly NIGHTLIFE_PEAK_BONUS = 120;
    private readonly NIGHTLIFE_EVENING_BONUS = 60;
    private readonly PANORAMA_GOLDEN_HOUR_BONUS = 40;
    private readonly TYPE_BONUS_SCORE = 70;
    private readonly NORMAL_BONUS_SCORE = 30;
    private readonly BUISNESS_BONUS_SCORE = 120;

    constructor(
        private readonly prisma: PrismaService,
        private readonly loc: LocalisationService,
        private readonly cdn: CdnService
    ) {}



    async suggestTrips(suggestTrip: SuggestTripDto, user: UserSession): Promise<TripSuggest> {
        const locCoords: Point = await this.loc.getCoordinates(suggestTrip.localisation);

        const locIds: number[] | null = await this.loc.getNearbyLocalisationIds(suggestTrip.localisation, 50);
        const candidates: PreStandardizedCandidate[] = await this.fetchCandidates(locIds);

        const standardizedCandidates: StandardizedPost[] = this.applyFallbacks(candidates);

        const weather: Weather = await this.weatherAdaptor(locCoords);

        const weatherFiltered: StandardizedPost[] = this.filterByWeather(standardizedCandidates, weather.code);

        const categories = [TripCategory.NORMAL, TripCategory.BUISNESS];

        let tripsByCategory = categories.map((category) =>
            this.buildPath(locCoords, suggestTrip, weatherFiltered, category, weather.code)
        ).map(trip => this.twoOptReorder(locCoords, trip, suggestTrip.transportMode));

        if (process.env.NODE_ENV === 'prod') {
            tripsByCategory = await Promise.all(
                tripsByCategory.map(trip =>
                    this.refineWithDirectionsAPI(locCoords, trip, suggestTrip.transportMode)
                )
            );
        }

        const tripDataByCategory = await Promise.all(
            categories.map((category, index) =>
                this.prisma.trip.create({
                    data: this.tripBuilder(tripsByCategory[index], category, suggestTrip.transportMode, suggestTrip.startingTime, weather.code, user.id),
                    select: { id: true }
                })
            )
        );

        const trips = tripsByCategory.map((trip, index) => ({ ...trip, id: tripDataByCategory[index].id }));

        return { trips, weather };
    }



    private async fetchCandidates(locIds: number[] | null): Promise<PreStandardizedCandidate[]> {
        if (!locIds || locIds.length === 0) return [];
            return this.prisma.post.findMany({
                where: { localisationId: { in: locIds }, groupId: null },
                include: { Localisation: true }
        });
    }



    private applyFallbacks(posts: PreStandardizedCandidate[]): StandardizedPost[] {
        return posts.map(post => {
            let resolvedMinDuration: number;
            let isDurationTrusted = true;
            let resolvedMinPrice: number;

            if (post.minDuration === null) {
                isDurationTrusted = false;

                switch (post.type) {
                    case PostType.PANORAMA: resolvedMinDuration = 30; break;
                    case PostType.HISTORIC_SITE:
                    case PostType.ART_CULTURE: resolvedMinDuration = 120; break;
                    case PostType.GASTRONOMY: resolvedMinDuration = 120; break;
                    case PostType.UNIQUE_STAY: resolvedMinDuration = 720; break;
                    default: resolvedMinDuration = 60;
                }
            } else {
                resolvedMinDuration = post.minDuration;
            }

            if (post.minPrice === null) {
                switch (post.type) {
                    case PostType.GASTRONOMY: resolvedMinPrice = 20; break;
                    case PostType.UNIQUE_STAY: resolvedMinPrice = 100; break;
                    case PostType.NIGHTLIFE: resolvedMinPrice = 15; break;
                    default: resolvedMinPrice = 0;
                }
            } else {
                resolvedMinPrice = post.minPrice;
            }

            const effectiveDuration = (resolvedMinDuration + (post.maxDuration ?? resolvedMinDuration)) / 2;
            const effectivePrice = (resolvedMinPrice + (post.maxPrice ?? resolvedMinPrice)) / 2;

            return { ...post, effectiveDuration, effectivePrice, isDurationTrusted };
        });
    }



    private filterByWeather(posts: StandardizedPost[], weather: WeatherCode): StandardizedPost[] {
        const OUTDOOR_TYPES: PostType[] = [PostType.PANORAMA, PostType.NATURAL_AREA, PostType.COASTAL_WATER, PostType.URBAN_ARCHITECTURE];
        const BAD_WEATHER: WeatherCode[] = [WeatherCode.RAIN, WeatherCode.SNOW, WeatherCode.STORM];

        if (BAD_WEATHER.includes(weather)) return posts.filter(post => !OUTDOOR_TYPES.includes(post.type));

        return posts;
    }



    private buildPath(start: Point, suggestTrip: SuggestTripDto, candidates: StandardizedPost[], category: TripCategory, weather: WeatherCode): TripSuggestInfos {
        const selectedPosts: TripStepDetail[] = [];
        let currentDuration = 0;
        let currentCost = 0;
        let currentLocation = start;
        let unvisited = [...candidates];
        const SAFE_MAX_DURATION = suggestTrip.maxDuration * 0.90;

        while (unvisited.length > 0) {
            const validCandidates: Candidate[] = [];

            for (const post of unvisited) {
                const postDuration = post.effectiveDuration;
                const postCost = post.effectivePrice;
                const postCoords = { lat: Number(post.Localisation.lat), long: Number(post.Localisation.long) };

                const travelTime = this.calculateTravelTime(currentLocation, postCoords, suggestTrip.transportMode);

                if (currentDuration + travelTime + postDuration <= SAFE_MAX_DURATION && currentCost + postCost <= suggestTrip.maxBudget) {
                    const elapsedMinutes: number = currentDuration + travelTime;
                    const dynamicScore = this.getDynamicScore(post, elapsedMinutes, suggestTrip.startingTime, category, suggestTrip.preferredTypes);
                    const ratio = dynamicScore / (travelTime + postDuration);

                    validCandidates.push({ post, ratio, travelTime });
                }
            }

            if (validCandidates.length === 0) break;

            const selected: Candidate = this.selectRandomCandidate(validCandidates, suggestTrip.isRegenerated);
            const bestNextPost = selected.post;
            const travelTimeToBest = selected.travelTime;

            const { Localisation, isDurationTrusted, ...cleanPost } = bestNextPost;

            selectedPosts.push({
                post: { ...cleanPost, image: this.cdn.getPostUrl(cleanPost.id, cleanPost.imageExt) },
                localisation: Localisation,
                travelTimeFromPrevious: travelTimeToBest,
                isTravelTimeFromPreviousTrusted: false,
                visitDuration: cleanPost.effectiveDuration,
                isVisitDurationTrusted: isDurationTrusted ?? false
            });
            currentDuration += travelTimeToBest + cleanPost.effectiveDuration;
            currentCost += cleanPost.effectivePrice;
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



    private getDynamicScore(post: StandardizedPost, currentElapsedMinutes: number, startingTime: TripStartingTime, category: TripCategory, preferredTypes?: PostType[]): number {
        let score = this.BASE_SCORE;

        const simulatedHour = this.getScoringHour(post.type, post.effectiveDuration, startingTime, currentElapsedMinutes);

        if (preferredTypes?.includes(post.type)) score += this.TYPE_BONUS_SCORE;

        switch (post.type) {
            case PostType.GASTRONOMY: {
                if (simulatedHour >= 12 && simulatedHour <= 14) score += this.HOUR_BONUS_SCORE;
                if (simulatedHour >= 19 && simulatedHour <= 21) score += this.HOUR_BONUS_SCORE;
                if (simulatedHour < 12 || (simulatedHour > 14 && simulatedHour < 19) || simulatedHour >= 21) score -= this.HOUR_MALUS_SCORE;
                break;
            }

            case PostType.NIGHTLIFE: {
                if (simulatedHour >= 22) score += this.NIGHTLIFE_PEAK_BONUS;
                else if (simulatedHour >= 18) score += this.NIGHTLIFE_EVENING_BONUS;
                else score -= this.HOUR_MALUS_SCORE;
                break;
            }

            case PostType.PANORAMA: {
                if ((simulatedHour >= 7 && simulatedHour <= 9) || (simulatedHour >= 17 && simulatedHour <= 19)) score += this.PANORAMA_GOLDEN_HOUR_BONUS;
                break;
            }
        }

        if (category === TripCategory.BUISNESS && (post.type === PostType.UNIQUE_STAY || post.type === PostType.GASTRONOMY)) score += this.BUISNESS_BONUS_SCORE;
        if (category === TripCategory.NORMAL && post.effectivePrice === 0) score += this.NORMAL_BONUS_SCORE;

        return score;
    }



    private getScoringHour(postType: PostType, effectiveDuration: number, startingTime: TripStartingTime, arrivalElapsedMinutes: number): number {
        const offset = postType === PostType.GASTRONOMY ? effectiveDuration / 2 : 0;
        return this.getSimulatedHour(startingTime, arrivalElapsedMinutes + offset);
    }



    private getSimulatedHour(startingTime: TripStartingTime, elapsedMinutes: number) {
        let baseHour: number;

        switch (startingTime) {
            case TripStartingTime.MORNING:   baseHour = 9; break;
            case TripStartingTime.MIDDAY:    baseHour = 12; break;
            case TripStartingTime.AFTERNOON: baseHour = 14; break;
            case TripStartingTime.EVENING:   baseHour = 19; break;
            default: baseHour = 9;
        }

        return (baseHour + elapsedMinutes / 60) % 24;
    }



    private getHaversineDistance(p1: Point, p2: Point): number {
        const R = 6371;
        const circleRatio = Math.PI / 180

        const dLat = (p2.lat - p1.lat) * circleRatio;
        const dLon = (p2.long - p1.long) * circleRatio;

        const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(p1.lat * circleRatio) * Math.cos(p2.lat * circleRatio) *
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
                return Math.round(drivingTime + parkingPenalty);
            }

            case TripTransportMode.WALK: return Math.round((urbanDistanceKm / 5) * 60);

            default: throw new BadRequestException('Undefined transport mode');
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



    private selectRandomCandidate(candidates: Candidate[], isRegenerated: boolean): Candidate {
        candidates.sort((a, b) => b.ratio - a.ratio);

        const RANDOM_POOL_SIZE = isRegenerated ? 3 : 1;
        const poolSize = Math.min(RANDOM_POOL_SIZE, candidates.length);
        const randomIndex = Math.floor(Math.random() * poolSize);

        return candidates[randomIndex];
    }



    private twoOptReorder(start: Point, trip: TripSuggestInfos, mode: TripTransportMode): TripSuggestInfos {
        if (trip.steps.length <= 2) return trip;

        const TIME_SENSITIVE = new Set<PostType>([PostType.GASTRONOMY, PostType.NIGHTLIFE]);
        const toPoint = (loc: Localisation): Point => ({ lat: Number(loc.lat), long: Number(loc.long) });

        const steps = [...trip.steps];
        let improved = true;

        while (improved) {
            improved = false;

            for (let i = 0; i < steps.length - 1; i++) {
                for (let j = i + 1; j < steps.length; j++) {
                    if (steps.slice(i, j + 1).some(s => TIME_SENSITIVE.has(s.post.type))) continue;

                    const prev = i === 0 ? start : toPoint(steps[i - 1].localisation);
                    const next = j < steps.length - 1 ? toPoint(steps[j + 1].localisation) : null;
                    const iPoint = toPoint(steps[i].localisation);
                    const jPoint = toPoint(steps[j].localisation);

                    const currentCost = this.calculateTravelTime(prev, iPoint, mode) + (next ? this.calculateTravelTime(jPoint, next, mode) : 0);
                    const newCost    = this.calculateTravelTime(prev, jPoint, mode) + (next ? this.calculateTravelTime(iPoint, next, mode) : 0);

                    if (newCost < currentCost) {
                        steps.splice(i, j - i + 1, ...steps.slice(i, j + 1).reverse());
                        improved = true;
                    }
                }
            }
        }

        let currentPoint = start;
        for (const step of steps) {
            const stepPoint = toPoint(step.localisation);
            step.travelTimeFromPrevious = this.calculateTravelTime(currentPoint, stepPoint, mode);
            currentPoint = stepPoint;
        }

        const totalTravel = steps.reduce((acc, s) => acc + s.travelTimeFromPrevious, 0);
        const totalVisit  = steps.reduce((acc, s) => acc + s.visitDuration, 0);

        return { ...trip, steps, totalDuration: totalTravel + totalVisit };
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

        try {
            const response = await fetch(`https://api.openrouteservice.org/v2/directions/${profile}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Authorization': this.ORS_API_KEY },
                body: JSON.stringify({ coordinates, units: 'm', elevation: true })
            });
            const data = await response.json();

            if (!data.routes || data.routes.length === 0) return trip;

            let totalTravelTime = 0;
            let totalDistanceMeters = 0;
            const segments = data.routes[0].segments;
            const summary = data.routes[0].summary;
            const totalAscent = summary.ascent ? Math.round(summary.ascent) : 0;

            trip.steps.forEach((step, index) => {
                const segment = segments[index];
                const travelMinutes = Math.round(segment.duration / 60);
                const distanceMeters = Math.round(segment.distance);

                step.travelTimeFromPrevious = travelMinutes;
                step.travelDistanceFromPrevious = distanceMeters;
                step.isTravelTimeFromPreviousTrusted = true;

                totalTravelTime += travelMinutes;
                totalDistanceMeters += distanceMeters;
            });

            const totalVisitTime = trip.steps.reduce((acc, step) => acc + step.visitDuration, 0);
            trip.totalDuration = totalTravelTime + totalVisitTime;
            trip.totalDistance = totalDistanceMeters;
            if (mode === TripTransportMode.WALK) trip.totalAscent = totalAscent;
            trip.difficulty = this.getDifficultyScore(totalDistanceMeters, totalAscent, mode);

            return trip;
        } catch (error) {
            return trip;
        }
    }



    private getDifficultyScore(totalDistanceMeters: number, totalAscentMeters: number, mode: TripTransportMode): number {
        if (mode === TripTransportMode.CAR) {
            return 1;
        }

        const distanceKm = totalDistanceMeters / 1000;
        const ascentEffort = totalAscentMeters / 100;
        const globalEffort = distanceKm + ascentEffort;

        if (globalEffort <= 2) return 1;
        if (globalEffort <= 6) return 2;
        if (globalEffort <= 12) return 3;
        if (globalEffort <= 18) return 4;

        return 5;
    }



    private tripBuilder(trip: TripSuggestInfos, category: TripCategory, transportMode: TripTransportMode, startingTime: TripStartingTime, weather: WeatherCode, userId: number) {
        return {
            budget: trip.totalCost,
            duration: trip.totalDuration,
            category: category,
            totalDistance: trip.totalDistance ?? null,
            difficulty: trip.difficulty ?? null,
            totalAscent: trip.totalAscent ?? null,
            startingTime: startingTime,
            transportMode: transportMode,
            weather,
            userId: userId,
            tripSteps: {
                create: trip.steps.map((step: any, index: number) => ({
                    postId: step.post.id,
                    stepNumber: index + 1,
                    travelTimeFromPrevious: step.travelTimeFromPrevious,
                    travelDistanceFromPrevious: step.travelDistanceFromPrevious ?? null,
                    isTravelTimeFromPreviousTrusted: step.isTravelTimeFromPreviousTrusted,
                    visitDuration: step.visitDuration,
                    isVisitDurationTrusted: step.isVisitDurationTrusted
                }))
            }
        };
    }
}