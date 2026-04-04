import { BadRequestException, CanActivate, ExecutionContext, ForbiddenException, Injectable, NotFoundException, UnauthorizedException } from '@nestjs/common';
import { PrismaService } from 'src/prisma/prisma.service';
import type { User } from '@prisma/client';
import { AuthService } from 'src/auth/auth.service';

@Injectable()
export class GroupProtectGuard implements CanActivate {
    constructor(
        private readonly prisma: PrismaService,
        private readonly authService: AuthService
    ) {}

    async canActivate(context: ExecutionContext): Promise<boolean> {
        const request = context.switchToHttp().getRequest();
        const commendId: number | null = request.params.commentId;
        const rawId: string | null = request.params.postId;

        if (!commendId && !rawId) {
            throw new BadRequestException('Invalid URI ressources');
        }

        let group: any = null;

        if (commendId) {
            const comment = await this.prisma.comment.findUnique({
                where: { id: Number(commendId) },
                include: { Post: { include: { Group: true } } },
            });

            if (!comment) {
                throw new NotFoundException('Invalid URI ressources');
            }

            group = comment.Post?.Group;
        }

        if (rawId) {
            const postId = rawId.split('.')[0];

            const post = await this.prisma.post.findUnique({
                where: { id: postId },
                include: { Group: true },
            });

            if (!post) {
                throw new NotFoundException('Invalid URI ressources');
            }

            group = post.Group;
        }

        let user: User | null = request['user'];
        if (!user) {
            const token: string | null = this.authService.extractBearerToken(request);
            if (token) {
                user = await this.authService.getUserSession(token);
                request['user'] = user;
            }
        }

        if (!group || !group.isGroupPrivate) {
            return true;
        }

        if (!user) {
            throw new UnauthorizedException('Authentication required for private group post');
        }

        if (!await this.prisma.member.findUnique({ where: { groupId_userId: { groupId: group.id, userId: user.id, }}})) {
            throw new ForbiddenException(`You're not in the group`);
        }

        return true;
    }
}
