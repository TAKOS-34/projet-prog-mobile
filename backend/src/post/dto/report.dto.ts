import { ReportReason } from "@prisma/client";
import { IsNotEmpty, IsOptional, MaxLength } from "class-validator";

export class ReportDto {
    @IsNotEmpty()
    reason!: ReportReason;

    @IsOptional()
    @MaxLength(300, { message: "Details must not be longer than 300 characters" })
    details?: string;
}