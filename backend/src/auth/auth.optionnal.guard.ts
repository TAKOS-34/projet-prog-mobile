import { BadRequestException, CanActivate, ExecutionContext, Injectable } from '@nestjs/common';
import { Request } from 'express';
import { UserSession } from 'src/utils/dto/userSession.dto';
import { AuthService } from './auth.service';

@Injectable()
export class AuthOptionalGuard implements CanActivate {
    constructor(private readonly authService: AuthService) {}

    async canActivate(context: ExecutionContext): Promise<boolean> {
        const request = context.switchToHttp().getRequest();
        const token: string | null = this.extractAnonymousToken(request);

        if (token && token.length === 36) {
            request['anonymous'] = token;
            return true;
        }

        const user: UserSession | null = request['user'];

        if (user) {
            return true;
        }

        const bearerToken = this.authService.extractBearerToken(request);
        if (bearerToken) {
            const user = await this.authService.getUserSession(bearerToken);
            if (user) {
                request['user'] = user;
                return true;
            }
        }

        throw new BadRequestException('Authentication required: Valid Bearer or Anonymous token expected');
    }

    private extractAnonymousToken(request: Request): string | null {
        const [type, token] = request.headers.authorization?.split(' ') ?? [];
        return type === 'Anonymous' ? token : null;
    }
}
