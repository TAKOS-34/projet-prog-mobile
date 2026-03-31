import { IsNotEmpty, IsOptional, MaxLength } from "class-validator";

export class PostDto {
    @IsNotEmpty()
    @MaxLength(64, { message: "Title must not be longer than 64 characters" })
    title: string;

    @IsNotEmpty()
    localisation: string;

    @IsOptional()
    @MaxLength(280, { message: "Description must not be longer than 280 characters" })
    description?: string;

    @IsOptional()
    groupId?: number;
}