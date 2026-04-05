import { BadRequestException, CanActivate, ExecutionContext, ForbiddenException, Injectable, NotFoundException, UnauthorizedException } from '@nestjs/common';
import { PrismaService } from 'src/prisma/prisma.service';
import { AuthService } from 'src/auth/auth.service';
import { UserSession } from 'src/utils/dto/userSession.dto';


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
        const audio: string | null = request.params.audio;
        const groupId: number | null = Number(request.params.groupId);

        if (!commendId && !rawId && !audio && !groupId) {
            throw new BadRequestException('Invalid URI ressources');
        }

        let group: { id: number; isGroupPrivate: boolean } | null = null;

        if (commendId) {
            const comment = await this.prisma.comment.findUniqueOrThrow({
                where: { id: Number(commendId) },
                select: {
                    Post: {
                        select: {
                            Group: {
                                select: {
                                    id: true,
                                    isGroupPrivate: true
                                }
                            }
                        }
                    }
                },
            });

            group = comment.Post?.Group;
        }

        if (rawId) {
            const postId = rawId.split('.')[0];

            const post = await this.prisma.post.findUniqueOrThrow({
                where: { id: postId },
                select: {
                    Group: {
                        select: {
                            id: true,
                            isGroupPrivate: true
                        }
                    }
                },
            });

            group = post.Group;
        }

        if (audio) {
            const post = await this.prisma.post.findFirstOrThrow({
                where: { audio },
                select: {
                    Group: {
                        select: {
                            id: true,
                            isGroupPrivate: true
                        }
                    }
                },
            });

            group = post.Group;
        }

        if (groupId) {
            group = await this.prisma.group.findUniqueOrThrow({
                where: { id: groupId },
                select: { id: true, isGroupPrivate: true }
            });
        }

        let user: UserSession | null = request['user'];
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

        if (!await this.prisma.member.findUnique({
            where: { groupId_userId: { groupId: group.id, userId: user.id } },
            select: { groupId: true }
        })) {
            throw new ForbiddenException(`You're not in the group`);
        }

        return true;
    }
}
