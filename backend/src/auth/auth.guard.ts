
import { CanActivate, ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';
import { PrismaService } from 'src/prisma/prisma.service';
import { UserToken } from '@prisma/client';
import { Request } from 'express';

@Injectable()
export class AuthGuard implements CanActivate {
    constructor(private readonly prisma: PrismaService) {}

    async canActivate(context: ExecutionContext): Promise<boolean> {
        const request = context.switchToHttp().getRequest();
        const token: string | null = this.extractTokenFromHeader(request);

        if (!token) throw new UnauthorizedException('Error during token verification');

        const session = await this.prisma.userToken.findUnique({
            where: { id: token },
            include: { User: true }
        });

        if (!session || new Date() > session.exprirationDate) {
            if (session) await this.prisma.userToken.delete({ where: { id: token } });
            throw new UnauthorizedException('Session expired or invalid');
        }

        request['user'] = session.User;
        return true;
    }

    private extractTokenFromHeader(request: Request): string | null {
        const [type, token] = request.headers.authorization?.split(' ') ?? [];
        return type === 'Bearer' ? token : null;
    }
}
