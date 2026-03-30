import { CanActivate, ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';
import { PrismaService } from 'src/prisma/prisma.service';
import { Request } from 'express';
import { AuthService } from './auth.service';

@Injectable()
export class AuthGuard implements CanActivate {
    constructor(
        private readonly prisma: PrismaService,
        private readonly authService: AuthService
    ) {}

    async canActivate(context: ExecutionContext): Promise<boolean> {
        const request = context.switchToHttp().getRequest();
        const token: string | null = this.extractTokenFromHeader(request);

        if (!token) {
            throw new UnauthorizedException('Error during token verification');
        }

        const hashedToken: string = this.authService.hashToken(token);

        const session = await this.prisma.userToken.findUnique({
            where: { hashedToken },
            include: { User: true }
        });

        if (!session || !session.User || new Date() > session.expirationDate) {
            if (session) await this.prisma.userToken.delete({ where: { hashedToken } });
            throw new UnauthorizedException('Error during token verification');
        }

        request['user'] = session.User;
        return true;
    }

    private extractTokenFromHeader(request: Request): string | null {
        const [type, token] = request.headers.authorization?.split(' ') ?? [];
        return type === 'Bearer' ? token : null;
    }
}
