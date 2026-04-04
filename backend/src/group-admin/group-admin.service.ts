import { BadRequestException, ConflictException, Injectable } from '@nestjs/common';
import { Prisma, type Group, type Member, type User } from '@prisma/client';
import { CdnService } from 'src/cdn/cdn.service';
import { AppMailerService } from 'src/mailer/mailer.service';
import { PrismaService } from 'src/prisma/prisma.service';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import * as fs from 'fs';
import { UpdateGroupDto } from './dto/updateGroup.dto';
import { randomUUID } from 'crypto';
import sharp from 'sharp';

@Injectable()
export class GroupAdminService {
    constructor(
        private readonly prisma: PrismaService,
        private readonly mailer: AppMailerService,
        private readonly cdn: CdnService
    ) {}



    async listRequest(groupId: number): Promise<any> {
        const request = await this.prisma.requestToJoin.findMany({
            where: { groupId },
            select: { User: { select: {
                id: true,
                username: true,
                avatar: true
            }}}
        });

        return request.map(user => ({
            id: user.User.id,
            username: user.User.username,
            avatar: this.cdn.getAvatarUrl(user.User.avatar)
        }));
    }



    async listBan(groupId: number): Promise<any> {
        const request = await this.prisma.ban.findMany({
            where: { groupId },
            select: { User: { select: {
                id: true,
                username: true,
                avatar: true
            }}}
        });

        return request.map(user => ({
            id: user.User.id,
            username: user.User.username,
            avatar: this.cdn.getAvatarUrl(user.User.avatar)
        }));
    }



    async accept(userId: number, group: Group): Promise<ResponseMessage> {
        await this.prisma.$transaction([
            this.prisma.requestToJoin.delete({
                where: {
                    groupId_userId: {
                        userId,
                        groupId: group.id
                    }
                }
            }),

            this.prisma.member.create({ data: { groupId: group.id, userId } })
        ]);

        return { status: true, message: 'Member accepted' };
    }



    async refuse(userId: number, group: Group): Promise<ResponseMessage> {
        await this.prisma.requestToJoin.delete({
            where: {
                groupId_userId: {
                    userId,
                    groupId: group.id
                }
            }
        });

        return { status: true, message: 'Member refused' };
    }



    async ban(userId: number, group: Group): Promise<ResponseMessage> {
        if (group.admin === userId) {
            throw new BadRequestException('You cannot ban the actual admin (yourself)');
        }

        await this.prisma.$transaction([
            this.prisma.member.delete({
                where: {
                    groupId_userId: {
                        userId,
                        groupId: group.id
                    }
                }
            }),

            this.prisma.ban.create({ data: {
                groupId: group.id,
                userId
            }})
        ]);

        const user: User | null = await this.prisma.user.findUnique({ where: { id: userId } });
        if (user) await this.mailer.sendGroupBanEmail(user.email, user.username, group.name);

        return { status: true, message: 'Member banned' };
    }



    async deban(userId: number, group: Group): Promise<ResponseMessage> {
        await this.prisma.ban.delete({
            where: {
                groupId_userId: {
                    groupId: group.id,
                    userId
                }
            }
        });

        return { status: true, message: 'Member debanned' };
    }



    async transferAdminRole(userId: number, group: Group, user: User): Promise<ResponseMessage> {
        const member: Member | null = await this.prisma.member.findUnique({
            where: {
                groupId_userId: {
                    groupId: group.id,
                    userId
                }
            }
        });

        if (!member) {
            throw new BadRequestException('The user is not in the group');
        }

        await this.prisma.group.update({
            where: { id: group.id },
            data: { admin: userId }
        });

        const newAdmin: User | null = await this.prisma.user.findUnique({ where: { id: userId } });
        if (newAdmin) {
            await this.mailer.sendTransferAdminRoleEmail(user.email, user.username, group.name, newAdmin.username);
            await this.mailer.sendReceiveAdminRoleEmail(newAdmin.email, newAdmin.username, group.name, user.username);
        }

        return { status: true, message: 'Admin role transfered' };
    }



    async updateGroup(updateGroup: UpdateGroupDto, group: Group): Promise<ResponseMessage> {
        const { name, description, isGroupPrivate } = updateGroup;

        if (await this.prisma.group.findUnique({ where: { name }})) {
            throw new BadRequestException('A group with this name already exists');
        }

        await this.prisma.group.update({
            where: { id: group.id },
            data: {
                ...(name && { name }),
                ...(description && { description }),
                ...(isGroupPrivate && { isGroupPrivate })
            }
        });

        return { status: true, message: 'Group updated' };
    }



    async updateGroupAvatar(group: Group, avatar: Express.Multer.File): Promise<ResponseMessage> {
        const avatarId: string = randomUUID() + '.jpg';
        const path: string = this.cdn.getGroupAvatarPath(avatarId);

        try {
            await sharp(avatar.buffer)
                .resize(500)
                .jpeg()
                .toFile(path);

            if (group.avatar) {
                await fs.promises.unlink(this.cdn.getGroupAvatarPath(group.avatar));
            }

            await this.prisma.group.update({
                where: { id: group.id },
                data: { avatar: avatarId }
            });

            return { status: true, message: 'Group avatar updated' };
        } catch (error) {
            throw new BadRequestException('Error during avatar update');
        }
    }



    async deleteGroup(group: Group): Promise<ResponseMessage> {
        try {
            await this.prisma.group.delete({
                where: { id: group.id }
            });
        } catch (error) {
            if (error instanceof Prisma.PrismaClientKnownRequestError && error.code === 'P2003') {
                throw new ConflictException('You need to delete all posts of the group before deleting it');
            }
        }

        if (group.avatar) {
            await fs.promises.unlink(this.cdn.getGroupAvatarPath(group.avatar));
        }

        return { status: true, message: 'Group deleted' };
    }
}
