import { Transform } from "class-transformer";
import { IsBoolean, IsOptional, MaxLength } from "class-validator";

export class UpdateGroupDto {
    @IsOptional()
    @MaxLength(32, { message: "Name must not be longer than 32 characters" })
    name?: string;

    @IsOptional()
    @Transform(({ value }: { value: any }) => value === 'true' || value === true)
    @IsBoolean()
    isGroupPrivate?: boolean;

    @IsOptional()
    @MaxLength(280, { message: "Description must not be longer than 280 characters" })
    description?: string;
}