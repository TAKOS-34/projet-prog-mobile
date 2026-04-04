import { BadRequestException, CanActivate, ExecutionContext, ForbiddenException, Injectable } from '@nestjs/common';
import type { Group, User } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';

@Injectable()
export class GroupAdminGuard implements CanActivate {
    constructor(private readonly prisma: PrismaService) {}

    async canActivate(context: ExecutionContext): Promise<boolean> {
        const request = context.switchToHttp().getRequest();
        const user: User | null = request.user;
        const groupId: number | null = parseInt(request.params.groupId);

        if (!groupId || !user) {
            throw new BadRequestException('Error during admin verification');
        }

        const group: Group | null = await this.prisma.group.findUnique({
            where: {
                id: groupId,
                admin: user.id
            }
        });

        if (!group) {
            throw new ForbiddenException(`You're not admin of the group`);
        }

        request['group'] = group;
        return true;
    }
}