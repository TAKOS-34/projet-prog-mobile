import { IsEmail, IsNotEmpty, IsStrongPassword, MinLength, MaxLength, IsOptional } from 'class-validator';

export class UpdateUserDto {
    @IsOptional()
    @IsEmail({}, { message: "Email must respect email format" })
    email?: string;

    @IsOptional()
    @IsNotEmpty()
    @MinLength(4, { message: "Username must be longer than or equal to 4 characters" })
    @MaxLength(32, { message: "Username must not be longer than 32 characters" })
    username?: string;

    @IsOptional()
    @IsNotEmpty()
    @IsStrongPassword(
        { minLength: 6, minSymbols: 0, minUppercase: 0 },
        { message: "Password must be at least 6 characters long with 1 number" }
    )
    password?: string;
}