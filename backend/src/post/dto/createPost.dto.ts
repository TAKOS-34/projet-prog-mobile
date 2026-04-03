import { IsNotEmpty, IsOptional, MaxLength, IsArray, IsString } from "class-validator";
import { Transform } from 'class-transformer';

export class CreatePostDto {
    @IsNotEmpty()
    @MaxLength(64, { message: "Title must not be longer than 64 characters" })
    title: string;

    @IsNotEmpty()
    localisation: string;

    @IsOptional()
    @MaxLength(280, { message: "Description must not be longer than 280 characters" })
    description?: string;

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
    tags: string[];
}