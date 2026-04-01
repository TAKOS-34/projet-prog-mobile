import { BadRequestException, Injectable } from '@nestjs/common';
import type { Group } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';

@Injectable()
export class GroupAdminService {
    constructor(private readonly prisma: PrismaService) {}

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

            this.prisma.member.create({ data: { groupId: group.id, userId } }),

            this.prisma.user.update({
                where: { id: userId },
                data: { nbGroups: { increment: 1 } }
            }),

            this.prisma.group.update({
                where: { id: group.id },
                data: { nbMembers: { increment: 1 } }
            })
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
            }}),

            this.prisma.group.update({
                where: { id: group.id },
                data: { nbMembers: { decrement: 1 } }
            }),

            this.prisma.user.update({
                where: { id: userId },
                data: { nbGroups: { decrement: 1 } }
            })
        ]);

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
}
