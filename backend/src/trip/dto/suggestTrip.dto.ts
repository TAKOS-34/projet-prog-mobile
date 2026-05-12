import { Type } from "class-transformer";
import { IsEnum, IsNotEmpty, IsOptional, Max, Min } from "class-validator";
import { PostType, TripStartingTime } from "@prisma/client";

export class SuggestTripDto {
    @IsNotEmpty()
    localisation!: string;

    @IsNotEmpty()
    @Type(() => Number)
    @Min(0)
    @Max(2000)
    maxBudget!: number;

    @IsNotEmpty()
    @Type(() => Number)
    @Min(60)
    @Max(720)
    maxDuration!: number;

    @IsNotEmpty()
    startingTime!: TripStartingTime

    @IsOptional()
    @IsEnum(PostType, { each: true })
    preferredTypes?: PostType[];
}