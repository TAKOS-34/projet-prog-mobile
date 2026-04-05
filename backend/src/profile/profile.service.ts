import { BadRequestException, Injectable } from '@nestjs/common';
import { randomUUID } from 'crypto';
import sharp from 'sharp';
import { PrismaService } from 'src/prisma/prisma.service';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { UpdateUserDto } from './dto/updateUser.dto';
import * as bcrypt from 'bcrypt';
import { AppMailerService } from 'src/mailer/mailer.service';
import { Token } from './dto/token.dto';
import type { UserProfile } from './dto/userProfile.dto';
import { CdnService } from 'src/cdn/cdn.service';
import * as fs from 'fs';
import { UserPublicProfile } from './dto/userPublicProfile.dto';
import type { UserSession } from 'src/utils/dto/userSession.dto';
import { UserGroup } from './dto/userGroup.dto';

@Injectable()
export class ProfileService {
    private saltRounds: number = Number(process.env.BCRYPT_SALT) ?? 10;

    constructor(
        private readonly prisma: PrismaService,
        private readonly mailer: AppMailerService,
        private readonly cdn: CdnService
    ) {}



    getProfile(user: UserSession): UserProfile {
        return {
            email: user.email,
            username: user.username,
            creationDate: user.creationDate,
            avatar: this.cdn.getAvatarUrl(user.avatar),
            nbGroups: user.nbGroups,
            nbPosts: user.nbPosts
        };
    }

    async getPublicProfile(userId: number): Promise<UserPublicProfile> {
        const user = await this.prisma.user.findUniqueOrThrow({
            where: { id: userId },
            select: { id: true, username: true, creationDate: true, avatar: true, nbGroups: true, nbPosts: true }
        });

        return {
            id: user.id,
            username: user.username,
            creationDate: user.creationDate,
            avatar: this.cdn.getAvatarUrl(user.avatar),
            nbGroups: user.nbGroups,
            nbPosts: user.nbPosts
        };
    }

    async getGroups(user: UserSession): Promise<UserGroup[]> {
        const groups = await this.prisma.group.findMany({
            where: { members: { some: { userId: user.id } } },
            select: { id: true, name: true, avatar: true, description: true, creationDate: true, isGroupPrivate: true, admin: true, nbMembers: true, nbPosts: true }
        });

        return groups.map(g => ({
            id: g.id,
            name: g.name,
            avatar: this.cdn.getGroupAvatarUrl(g.avatar),
            description: g.description ?? undefined,
            creationDate: g.creationDate,
            isGroupPrivate: g.isGroupPrivate,
            nbMembers: g.nbMembers,
            nbPosts: g.nbPosts,
            isAdmin: g.admin === user.id
        }));
    }

    async getTokens(user: UserSession): Promise<Token[]> {
        return await this.prisma.userToken.findMany({
            where: { userId: user.id },
            select: { id: true, creationDate: true, ip: true, device: true }
        });
    }



    async updateAvatar(avatar: Express.Multer.File, user: UserSession): Promise<ResponseMessage> {
        const avatarId: string = randomUUID() + '.jpg';
        const path: string = this.cdn.getAvatarPath(avatarId);

        try {
            await sharp(avatar.buffer)
                .resize(500)
                .jpeg()
                .toFile(path);

            if (user.avatar) {
                await fs.promises.unlink(this.cdn.getAvatarPath(user.avatar));
            }

            await this.prisma.user.update({
                where: { id: user.id },
                data: { avatar: avatarId },
                select: { id: true }
            });

            return { status: true, message: 'Avatar updated' };
        } catch (error) {
            throw new BadRequestException('Error during avatar update');
        }
    }



    async updateInfos(updateUserDto: UpdateUserDto, user: UserSession): Promise<ResponseMessage> {
        const { email, username, password } = updateUserDto;

        if (email && await this.prisma.user.findUnique({ where: { email }, select: { id: true } })) {
            throw new BadRequestException('Email already taken');
        }

        if (username && await this.prisma.user.findUnique({ where: { username }, select: { id: true } })) {
            throw new BadRequestException('Username already taken');
        }

        if (password) {
            updateUserDto.password = await bcrypt.hash(password, this.saltRounds);

            await this.prisma.userToken.deleteMany({
                where: { userId: user.id }
            });
        }

        await this.prisma.user.update({
            where: { id: user.id },
            data: {
                ...(email && { email }),
                ...(username && { username }),
                ...(password && { password: updateUserDto.password }),
            },
            select: { id: true }
        });

        await this.mailer.sendUpdatedProfileEmail(user.email, user.username, { emailChanged: !!email, usernameChanged: !!username, passwordChanged: !!password });

        const message: string = password ? 'Password changed, please reconnecte to your account' : 'Profile updated';
        return { status: true, message };
    }



    async deleteAccount(user: UserSession): Promise<ResponseMessage> {
        const deletedUser = await this.prisma.user.delete({
            where: { id: user.id },
            select: {
                avatar: true,
                userPosts: {
                    select: {
                        id: true,
                        imageExt: true
                    }
                }
            }
        });

        const pathsToDelete = deletedUser.userPosts.map(
            post => this.cdn.getPostPath(`${post.id}.${post.imageExt}`)
        );

        if (deletedUser.avatar) {
            pathsToDelete.push(this.cdn.getAvatarPath(deletedUser.avatar));
        }

        await Promise.allSettled(
            pathsToDelete.map(path => fs.promises.unlink(path))
        );

        await this.mailer.sendDeleteAccountEmail(user.email, user.username);

        return { status: true, message: 'Account deleted' };
    }




    async deleteToken(tokenId: number, user: UserSession): Promise<ResponseMessage> {
        await this.prisma.userToken.deleteMany({
            where: {
                id: tokenId,
                userId: user.id
            }
        });

        return { status: true, message: 'Session removed' };
    }
}
