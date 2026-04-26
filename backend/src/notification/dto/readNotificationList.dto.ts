import { IsNotEmpty } from "class-validator";

export class ReadNotificationListDto {
    @IsNotEmpty()
    notificationIds!: number[];
}