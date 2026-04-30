import { PostType } from "@prisma/client";
import { IsOptional, MaxLength, IsArray, IsString } from "class-validator";

export class UpdatePostDto {
    @IsOptional()
    @MaxLength(64, { message: "Title must not be longer than 64 characters" })
    title?: string

    @IsOptional()
    type?: PostType;

    @IsOptional()
    @MaxLength(280, { message: "Description must not be longer than 280 characters" })
    description?: string;
}