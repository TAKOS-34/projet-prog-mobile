import { BadRequestException, Injectable } from '@nestjs/common';
import { randomUUID } from 'crypto';
import sharp from 'sharp';
import { PrismaService } from 'src/prisma/prisma.service';
import { unlink } from 'fs';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import type { User } from '@prisma/client';
import { UpdateUserDto } from './dto/updateUser.dto';
import * as bcrypt from 'bcrypt';
import { AppMailerService } from 'src/mailer/mailer.service';
import { Token } from './dto/token.dto';
import type { UserProfile } from './dto/userProfile.dto';
import { CdnService } from 'src/cdn/cdn.service';

@Injectable()
export class ProfileService {
    private saltRounds: number = Number(process.env.BCRYPT_SALT) ?? 10;

    constructor(
        private readonly prisma: PrismaService,
        private readonly mailer: AppMailerService,
        private readonly cdn: CdnService
    ) {}



    getProfile(user: User): UserProfile {
        return {
            email: user.email,
            username: user.username,
            creationDate: user.creationDate,
            avatar: this.cdn.getAvatarUrl(user.avatar),
            nbGroups: user.nbGroups,
            nbPosts: user.nbPosts
        };
    }



    async updateAvatar(avatar: Express.Multer.File, user: User): Promise<ResponseMessage> {
        const avatarId: string = randomUUID() + '.jpg';
        const url: string = this.cdn.getAvatarPath(avatarId);

        try {
            await sharp(avatar.buffer)
                .resize(500)
                .jpeg()
                .toFile(url);

            if (user.avatar) {
                const oldAvatarUrl = this.cdn.getAvatarPath(user.avatar);
                unlink(oldAvatarUrl, () => {});
            }

            await this.prisma.user.update({
                where: { id: user.id },
                data: { avatar: avatarId }
            });

        return { status: true, message: "Avatar updated" };
        } catch (error) {
            throw new BadRequestException('Error during avatar update');
        }
    }



    async updateInfos(updateUserDto: UpdateUserDto, user: User): Promise<ResponseMessage> {
        const { email, username, password } = updateUserDto;

        if (email && await this.prisma.user.findUnique({ where: { email } })) {
            throw new BadRequestException('Email already taken');
        }

        if (username && await this.prisma.user.findUnique({ where: { username } })) {
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
            data: updateUserDto
        });

        await this.mailer.sendUpdatedProfileEmail(user.email, user.username, { emailChanged: !!email, usernameChanged: !!username, passwordChanged: !!password });

        const message: string = password ? "Password changed, please reconnecte to your account" : "Profile updated";
        return { status: true, message };
    }



    async deleteAccount(user: User): Promise<ResponseMessage> {
        if (user.avatar) {
            unlink(this.cdn.getAvatarPath(user.avatar), () => {});
        }

        await this.prisma.user.delete({
            where: { id: user.id }
        });

        await this.mailer.sendDeleteAccountEmail(user.email, user.username);

        return { status: true, message: "Account deleted" };
    }



    async getTokens(user: User): Promise<Array<Token>> {
        return await this.prisma.userToken.findMany({
            where: { userId: user.id },
            select: {
                id: true,
                creationDate: true,
                ip: true,
                device: true
            }
        });
    }



    async deleteToken(tokenId: number, user: User): Promise<ResponseMessage> {
        try {
            await this.prisma.userToken.deleteMany({
                where: {
                    id: tokenId,
                    userId: user.id
                }
            });
        } catch (error) {
            throw new BadRequestException('Error during session suppression');
        }

        return { status: true, message: "Session removed" };
    }
}
