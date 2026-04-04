import { IsOptional, MaxLength, IsArray, IsString } from "class-validator";

export class UpdatePostDto {
    @IsOptional()
    @MaxLength(64, { message: "Title must not be longer than 64 characters" })
    title?: string

    @IsOptional()
    localisation?: string;

    @IsOptional()
    @MaxLength(280, { message: "Description must not be longer than 280 characters" })
    description?: string;
}