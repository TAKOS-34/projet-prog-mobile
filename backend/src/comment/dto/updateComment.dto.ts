import { IsNotEmpty, MaxLength } from "class-validator";

export class UpdateCommentDto {
    @IsNotEmpty()
    @MaxLength(280, { message: "Content must not be longer than 280 characters" })
    content!: string;
}