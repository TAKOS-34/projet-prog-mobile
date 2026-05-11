import { IsNotEmpty, IsOptional, MaxLength, IsArray, IsString, Min, Max } from "class-validator";
import { Transform, Type } from 'class-transformer';
import { PostType } from "@prisma/client";

export class CreatePostDto {
    @IsNotEmpty()
    @MaxLength(64, { message: "Title must not be longer than 64 characters" })
    title!: string;

    @IsNotEmpty()
    localisation!: string;

    @IsNotEmpty()
    type!: PostType;

    @IsOptional()
    @MaxLength(280, { message: "Description must not be longer than 280 characters" })
    description?: string;

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
    @Transform(({ value }) => Number(value))
    groupId?: number;

    @IsOptional()
    @IsArray()
    @IsString({ each: true })
    @Transform(({ value }) => {
        if (typeof value === 'string') {
            try { return JSON.parse(value); }
            catch { return [value]; }
        }
        return value;
    })
    tags?: string[];
}