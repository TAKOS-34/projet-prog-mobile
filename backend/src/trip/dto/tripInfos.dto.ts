import { Localisation, Post, TripCategory, TripStartingTime, TripTransportMode, WeatherCode } from "@prisma/client";

export type TripSuggest = {
    trips: TripSuggestInfos[];
    weather: Weather;
}

export type TripInfos = {
    trips: TripDto[];
    nextCursor?: number;
}

export interface TripDto extends TripSuggestInfos {
    startLocalisation?: Localisation;
    creationDate: Date;
    category: TripCategory;
    startingTime: TripStartingTime;
    transportMode: TripTransportMode;
    nbLikes: number;
    nbBookmarks: number;
    isLiked: boolean;
    isBookmarked: boolean;
    isYours: boolean;
    userId: number;
    username: string;
    avatar: string;
}

export interface TripSuggestInfos {
    id?: number;
    steps: TripStepDetail[];
    totalDuration: number;
    totalCost: number;
    totalStep: number;
    totalDistance: number | null;
    weather: WeatherCode;
    difficulty?: number;
}

export interface TripStepDetail {
    post: Post & { image: string };
    localisation: Localisation;
    travelTimeFromPrevious: number;
    travelDistanceFromPrevious: number | null;
    isTravelTimeFromPreviousTrusted: boolean;
    visitDuration: number;
    isVisitDurationTrusted: boolean;
}

export interface ScoredPost extends Post {
    score: number;
    Localisation: Localisation;
}

export type Weather = {
    code: WeatherCode
    temperature: number;
}