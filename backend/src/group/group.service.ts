import { BadRequestException, Injectable, UnauthorizedException } from '@nestjs/common';
import type { User, Group, Ban } from '@prisma/client';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { CreateGroupDto } from './dto/createGroup.dto';
import { CdnService } from 'src/cdn/cdn.service';
import { PrismaService } from 'src/prisma/prisma.service';
import { randomUUID } from 'crypto';
import sharp from 'sharp';

@Injectable()
export class GroupService {
    constructor(
        private readonly prisma: PrismaService,
        private readonly cdn: CdnService
    ) {}



    async createGroup(group: CreateGroupDto, user: User, avatar?: Express.Multer.File): Promise<ResponseMessage> {
        const existingGroup: Group | null = await this.prisma.group.findUnique({
            where: { name: group.name }
        });

        if (existingGroup) {
            throw new BadRequestException('A group with this name already exists');
        }

        const avatarId: string | null = avatar ? randomUUID() + '.jpg' : null;
        const avatarPath: string | null = avatarId ? this.cdn.getGroupAvatarPath(avatarId) : null;

        try {
            const newGroup: Group = await this.prisma.group.create({ data: {
                name: group.name,
                description: group.description ?? null,
                avatar: avatarId,
                isGroupPrivate: group.isGroupPrivate,
                admin: user.id
            }});

            await this.prisma.member.create({ data: {
                groupId: newGroup.id,
                userId: user.id
            }});

            if (avatar && avatarId && avatarPath) {
                await sharp(avatar.buffer)
                    .resize(500)
                    .jpeg()
                    .toFile(avatarPath);
            }
        } catch (error) {
            throw new BadRequestException('Error during group creation');
        }

        return { status: true, message: 'Group created' };
    }



    async requestToJoin(groupId: number, user: User): Promise<ResponseMessage> {
        const group = await this.prisma.group.findUnique({
            where: { id: groupId },
            include: { members: true }
        });

        if (!group) {
            throw new BadRequestException('This group does not exist');
        }

        if (group.members?.some(member => member.userId === user.id)) {
            throw new BadRequestException('You are already a member of this group');
        }

        const isUserBanned: Ban | null = await this.prisma.ban.findFirst({
            where: {
                groupId,
                userId: user.id
            }
        });

        if (isUserBanned) {
            throw new UnauthorizedException('Your banned from this group');
        }

        if (group.isGroupPrivate) {
            await this.prisma.requestToJoin.create({ data: {
                groupId,
                userId: user.id
            }});

            return { status: true, message: 'A request to join the group has been sent' };
        } else {
            await this.prisma.$transaction([
                this.prisma.member.create({ data : { groupId, userId: user.id }}),

                this.prisma.user.update({
                    where: { id: user.id },
                    data: { nbGroups: { increment: 1 } }
                })
            ]);

            return { status: true, message: 'Group join' };
        }
    }



    async quit(groupId: number, user: User): Promise<ResponseMessage> {
        const isUserAdmin: Group | null = await this.prisma.group.findUnique({
            where: {
                id: groupId,
                admin: user.id
            }
        });

        if (isUserAdmin) {
            throw new BadRequestException('You cannot quit the group as group admin');
        }

        await this.prisma.$transaction([
            this.prisma.member.delete({
                where: {
                    groupId_userId: {
                        userId: user.id,
                        groupId
                    }
                }
            }),

            this.prisma.group.update({
                where: { id: groupId },
                data: { nbMembers: { decrement: 1 } }
            }),

            this.prisma.user.update({
                where: { id: user.id },
                data: { nbGroups: { decrement: 1 } }
            })
        ]);

        return { status: true, message: 'Group quited' };
    }
}
