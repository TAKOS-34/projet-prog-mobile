import { PrismaModule } from "src/prisma/prisma.module";
import { DbCronService } from "./db.cron.service";
import { Module } from "@nestjs/common";

@Module({
    imports: [PrismaModule],
    providers: [DbCronService]
})

export class DbCronModule {};