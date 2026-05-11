import { PostType } from "@prisma/client";
import { Type } from "class-transformer";
import { IsOptional, MaxLength, Min, Max } from "class-validator";

export class UpdatePostDto {
    @IsOptional()
    @MaxLength(64, { message: "Title must not be longer than 64 characters" })
    title?: string

    @IsOptional()
    type?: PostType;

    @IsOptional()
    @Type(() => Number)
    @Min(0)
    minPrice?: number;
    
    @IsOptional()
    @Type(() => Number)
    @Min(0)
    @Max(1000)
    maxPrice?: number;

    @IsOptional()
    @Type(() => Number)
    @Min(0)
    minDuration?: number;

    @IsOptional()
    @Type(() => Number)
    @Min(0)
    @Max(480)
    maxDuration?: number;

    @IsOptional()
    @MaxLength(280, { message: "Description must not be longer than 280 characters" })
    description?: string;
}