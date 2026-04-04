import { BadRequestException, CanActivate, ExecutionContext, Injectable } from '@nestjs/common';
import type { User } from '@prisma/client';
import { Request } from 'express';

@Injectable()
export class AuthLikeGuard implements CanActivate {
    constructor() {}

    async canActivate(context: ExecutionContext): Promise<boolean> {
        const request = context.switchToHttp().getRequest();
        const user: User | null = request['user'];

        if (user) {
            return true;
        }

        const token: string | null = this.extractAnonymousToken(request);

        if (!token || token.length != 36) {
            throw new BadRequestException('You need an anonymous token');
        }

        request['anonymous'] = token;
        return true;
    }

    private extractAnonymousToken(request: Request): string | null {
        const [type, token] = request.headers.authorization?.split(' ') ?? [];
        return type === 'Anonymous' ? token : null;
    }
}
