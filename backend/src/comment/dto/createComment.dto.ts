import { Transform } from "class-transformer";
import { IsNotEmpty, IsOptional, MaxLength } from "class-validator";

export class CreateCommentDto {
    @IsNotEmpty()
    @MaxLength(280, { message: "Content must not be longer than 280 characters" })
    content!: string;

    @IsOptional()
    @Transform(({ value }) => Number(value))
    parentId?: number;
}