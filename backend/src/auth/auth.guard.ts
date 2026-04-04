import { CanActivate, ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';
import { PrismaService } from 'src/prisma/prisma.service';
import { AuthService } from './auth.service';
import type { User } from '@prisma/client';

@Injectable()
export class AuthGuard implements CanActivate {
    constructor(private readonly authService: AuthService) {}

    async canActivate(context: ExecutionContext): Promise<boolean> {
        const request = context.switchToHttp().getRequest();
        const token: string | null = this.authService.extractBearerToken(request);
        const user: User | null = await this.authService.getUserSession(token);

        if (!user) {
            throw new UnauthorizedException('Error during token verification');
        }

        request['user'] = user;
        return true;
    }
}
