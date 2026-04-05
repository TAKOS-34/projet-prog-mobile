import { BadRequestException, CanActivate, ExecutionContext, ForbiddenException, Injectable } from '@nestjs/common';
import type { UserSession } from 'src/utils/dto/userSession.dto';
import { PrismaService } from 'src/prisma/prisma.service';

@Injectable()
export class GroupAdminGuard implements CanActivate {
    constructor(private readonly prisma: PrismaService) {}

    async canActivate(context: ExecutionContext): Promise<boolean> {
        const request = context.switchToHttp().getRequest();
        const user: UserSession | null = request.user;
        const groupId: number | null = parseInt(request.params.groupId);

        if (!groupId || !user) {
            throw new BadRequestException('Error during admin verification');
        }

        const group = await this.prisma.group.findUnique({
            where: {
                id: groupId,
                admin: user.id
            },
            select: {
                id: true,
                admin: true,
                name: true,
                avatar: true
            }
        });

        if (!group) {
            throw new ForbiddenException(`You're not admin of the group`);
        }

        request['group'] = group;
        return true;
    }
}