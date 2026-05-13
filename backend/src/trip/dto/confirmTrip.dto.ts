import { IsNotEmpty } from "class-validator";

export class ConfirmTripDto {
    @IsNotEmpty()
    localisation!: string;
}