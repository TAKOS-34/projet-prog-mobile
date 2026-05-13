import { Localisation, Post } from "@prisma/client";

export type TripSuggestResponse = {
    trips: TripSuggestInfos[];
    weather: Weather;
}

export type TripSuggestInfos = {
    id?: number;
    steps: TripStepDetail[];
    totalDuration: number;
    totalCost: number;
    totalStep: number;
    difficulty?: number;
}

export type TripStepDetail = {
    post: Post;
    localisation: Localisation;
    travelTimeFromPrevious: TripTime;
    visitDuration: TripTime;
}

export type TripTime = {
    time: number;
    trusted: boolean;
}

export interface ScoredPost extends Post {
    score: number;
    Localisation: Localisation;
}

export enum WeatherCode {
    CLEAR = 'CLEAR',
    CLOUDY = 'CLOUDY',
    RAIN = 'RAIN',
    STORM = 'STORM',
    SNOW = 'SNOW'
}

export type Weather = {
    code: WeatherCode
    temperature: number;
}