import { BadRequestException, Injectable, NotFoundException, UnauthorizedException } from '@nestjs/common';
import type { CreatePostDto } from './dto/createPost.dto';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import type { UserSession } from 'src/utils/dto/userSession.dto';
import { PrismaService } from 'src/prisma/prisma.service';
import NodeGeocoder from 'node-geocoder';
import sharp from 'sharp';
import { CdnService } from 'src/cdn/cdn.service';
import * as fs from 'fs';
import { ReportDto } from './dto/report.dto';
import { UpdatePostDto } from './dto/updatePost.dto';
import { randomUUID } from 'crypto';
import { NotificationService } from 'src/notification/notification.service';
import { parseBuffer } from 'music-metadata';
import { LocalisationUtil } from 'src/utils/localisation/localisation.util';

@Injectable()
export class PostCommandService {
    constructor(
        private readonly prisma: PrismaService,
        private readonly cdn: CdnService,
        private readonly notification: NotificationService,
        private readonly locUtil: LocalisationUtil
    ) {}


    async createPost(image: Express.Multer.File, post: CreatePostDto, user: UserSession, audio?: Express.Multer.File): Promise<ResponseMessage> {
        const cleanLocalisation = post.localisation.toLowerCase().trim();
        const { long, lat } = await this.locUtil.getCoordinates(cleanLocalisation);
        const imageExt: string = image.mimetype.replace('image/', '');
        const cleanTags: string[] = (post.tags ?? []).map(t => t.toLowerCase().trim().split(/\s+/)[0]).filter(t => t !== '');
        const audioName = audio ? (randomUUID() + '.' + audio.mimetype.replace('audio/', '')) : null;
        const audioDurationMs = audio ? await this.extractAudioDuration(audio.buffer, audio.mimetype) : null;

        if (audioName && !audioDurationMs) {
            throw new BadRequestException('Error with audio file');
        }

        if (post.groupId && !await this.prisma.member.findUnique({
            where: { groupId_userId: { groupId: post.groupId, userId: user.id } },
            select: { groupId: true }
        })) {
            throw new UnauthorizedException(`You're not in the group`);
        }

        try {

            const localisation = await this.prisma.localisation.upsert({
                where: { name: cleanLocalisation }, update: {},
                create: { name: cleanLocalisation, long: long, lat: lat }
            });

            const newPost = await this.prisma.post.create({ data: {
                imageExt,
                userId: user.id,
                title: post.title,
                type: post.type,
                description: post.description ?? null,
                groupId: post.groupId ?? null,
                audio: audioName,
                audioDuration: audioDurationMs,
                localisationId: localisation.id,
                postTags: {
                    create: cleanTags.map(tagName => ({
                        tag: {
                            connectOrCreate: {
                                where: { name: tagName },
                                create: { name: tagName }
                            }
                        }
                    }))
                }},
                select: { id: true }
            });

            const imagePath: string = newPost.id + '.' + imageExt;
            const postUrl = this.cdn.getPostPath(imagePath);

            await sharp(image.buffer).toFile(postUrl);

            if (audio && audioName) {
                const audioPath: string = this.cdn.getAudioPath(audioName);
                await fs.promises.writeFile(audioPath, audio.buffer);
            }

            this.notification.notifyNewPostUser(user.id, newPost.id, post.title, post.groupId);
            this.notification.notifyNewPostLocalisation(user.id, newPost.id, post.title, cleanLocalisation, post.groupId);

            if (post.groupId) {
                this.notification.notifyNewPostGroup(post.groupId, user.id, newPost.id, post.title);
            }

            if (cleanTags.length > 0) {
                this.notification.notifyNewPostTags(cleanTags, user.id, newPost.id, post.title, post.groupId);
            }
        } catch (error) {
            console.error(error)
            throw new BadRequestException('Error during post creation');
        }

        return { status: true, message: 'New post created' };
    }



    async updatePost(postId: string, post: UpdatePostDto, user: UserSession): Promise<ResponseMessage> {
        const postExists = await this.prisma.post.findUnique({
            where: { id: postId },
            select: { userId: true }
        });

        if (!postExists) {
            throw new NotFoundException('Post does not exist');
        }

        if (postExists.userId !== user.id) {
            throw new UnauthorizedException('You can only update your own posts');
        }

        const { title, type, description } = post;

        const updateData: any = {
            isEdited: true,
            updatedAt: new Date(),
            ...(title && { title }),
            ...(description && { description }),
            ...(type && { type }),
        };

        await this.prisma.post.update({
            data: updateData,
            where: { id: postId }
        });

        return { status: true, message: 'Post updated' };
    }



    async deletePost(postId: string, user: UserSession): Promise<ResponseMessage> {
        const post = await this.prisma.post.findFirst({
            where: {
                id: postId,
                OR: [
                    { userId: user.id },
                    { Group: { adminId: user.id } }
                ]
            },
            select: { id: true, imageExt: true, audio: true }
        });

        if (!post) {
            throw new NotFoundException('Post does not exist');
        }

        await this.prisma.post.delete({ where: { id: post.id } });

        const imageName: string = postId + '.' + post.imageExt;
        const imagePath: string = this.cdn.getPostPath(imageName);
        await fs.promises.unlink(imagePath);

        if (post.audio) {
            const audioPath: string = this.cdn.getAudioPath(post.audio);
            await fs.promises.unlink(audioPath);
        }

        return { status: true, message: 'Post deleted' };
    }



    async reportPost(postId: string, report: ReportDto, user: UserSession): Promise<ResponseMessage> {
        await this.prisma.report.create({ data: {
            reason: report.reason,
            details: report.details ?? null,
            postId,
            userId: user.id
        }});

        // Notification to admins

        return { status: true, message: 'Post reported' };
    }



    private async extractAudioDuration(buffer: Buffer, mimetype: string): Promise<number | null> {
        try {
            const metadata = await parseBuffer(buffer, { mimeType: mimetype });
            const seconds = metadata.format.duration;
            return typeof seconds === 'number' && Number.isFinite(seconds) ? Math.round(seconds * 1000) : null;
        } catch {
            return null;
        }
    }
}
