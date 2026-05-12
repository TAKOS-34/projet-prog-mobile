import { Localisation, Post } from "@prisma/client";

export type TripSuggestResponse = {
    trips: TripSuggestInfos[];
    weather: Weather;
}

export type TripSuggestInfos = {
    id?: number;
    steps: ScoredPost[];
    totalDuration: number;
    totalCost: number;
    totalStep: number;
    difficulty?: number;
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