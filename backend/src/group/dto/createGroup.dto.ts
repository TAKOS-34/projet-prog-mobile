import { IsNotEmpty, IsOptional, MaxLength, IsBoolean } from "class-validator";
import { Transform } from "class-transformer";

export class CreateGroupDto {
    @IsNotEmpty()
    @MaxLength(32, { message: "Name must not be longer than 32 characters" })
    name: string;

    @Transform(({ value }: { value: any }) => value === 'true' || value === true)
    @IsBoolean()
    isGroupPrivate: boolean;

    @IsOptional()
    description?: string;
}