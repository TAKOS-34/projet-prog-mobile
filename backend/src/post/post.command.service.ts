import { BadRequestException, Injectable, NotFoundException, UnauthorizedException } from '@nestjs/common';
import type { CreatePostDto } from './dto/createPost.dto';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { Post, User } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';
import NodeGeocoder from 'node-geocoder';
import sharp from 'sharp';
import { CdnService } from 'src/cdn/cdn.service';
import * as fs from 'fs';
import { ReportDto } from './dto/report.dto';
import { UpdatePostDto } from './dto/updatePost.dto';
import { randomUUID } from 'crypto';

@Injectable()
export class PostCommandService {
    private readonly geocoder: NodeGeocoder.Geocoder = NodeGeocoder({ provider: 'openstreetmap' });

    constructor(
        private readonly prisma: PrismaService,
        private readonly cdn: CdnService
    ) {}


    async createPost(image: Express.Multer.File, post: CreatePostDto, user: User, audio?: Express.Multer.File): Promise<ResponseMessage> {
        const { long, lat } = await this.getCoordinates(post.localisation);
        const imageExt: string = image.mimetype.replace('image/', '');
        const cleanTags: string[] = (post.tags ?? []).map(t => t.toLowerCase().trim());
        const audioName = audio ? (randomUUID() + '.' + audio.mimetype.replace('audio/', '')) : null;

        if (post.groupId && !await this.prisma.member.findUnique({ where: { groupId_userId: { groupId: post.groupId, userId: user.id} } })) {
            throw new UnauthorizedException(`You're not in the group`);
        }

        try {
            const newPost: Post = await this.prisma.post.create({ data: {
                imageExt,
                lat,
                long,
                userId: user.id,
                title: post.title,
                localisation: post.localisation,
                description: post.description ?? null,
                groupId: post.groupId ?? null,
                audio: audioName,
                postTags: {
                    create: cleanTags.map(tagName => ({
                        tag: {
                            connectOrCreate: {
                                where: { name: tagName },
                                create: { name: tagName }
                            }
                        }
                    }))
                }
            }});

            const imagePath: string = newPost.id + '.' + imageExt;
            const postUrl = this.cdn.getPostPath(imagePath);

            await sharp(image.buffer).toFile(postUrl);

            if (audio && audioName) {
                const audioPath: string = this.cdn.getAudioPath(audioName);
                await fs.promises.writeFile(audioPath, audio.buffer);
            }
        } catch (error) {
            throw new BadRequestException('Error during post creation');
        }

        return { status: true, message: 'New post created' };
    }



    async updatePost(postId: string, post: UpdatePostDto, user: User): Promise<ResponseMessage> {
        const { title, description, localisation } = post;
        const coords = localisation ? await this.getCoordinates(localisation) : undefined;

        await this.prisma.post.updateMany({
            data: {
                isEdited: true,
                updatedAt: new Date(),
                ...(title && { title }),
                ...(description && { description }),
                ...(localisation && { localisation }),
                ...(coords && { long: coords.long, lat: coords.lat })
            },
            where: {
                id: postId,
                userId: user.id,
            }
        });

        return { status: true, message: 'Post updated' };
    }



    async deletePost(postId: string, user: User): Promise<ResponseMessage> {
        const post = await this.prisma.post.findFirst({
            where: {
                id: postId,
                OR: [
                    { userId: user.id },
                    { Group: { admin: user.id } }
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



    async reportPost(postId: string, report: ReportDto, user: User): Promise<ResponseMessage> {
        await this.prisma.report.create({ data: {
            reason: report.reason,
            details: report.details ?? null,
            postId,
            userId: user.id
        }});

        // Notification to admins

        return { status: true, message: 'Post reported' };
    }



    private async getCoordinates(address: string): Promise<{ long: number, lat: number }> {
        const res = await this.geocoder.geocode(address);

        if (!res.length || !res[0].latitude || !res[0].longitude) {
            throw new BadRequestException('Localisation is not correct');
        }

        return { long: res[0].longitude, lat: res[0].latitude };
    }
}
