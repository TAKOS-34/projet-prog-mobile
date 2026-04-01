import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import type { CreatePostDto } from './dto/createPost.dto';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { Post, User } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';
import NodeGeocoder from 'node-geocoder';
import sharp from 'sharp';
import { CdnService } from 'src/cdn/cdn.service';
import { PostInfos } from './dto/postInfos.dto';
import * as fs from 'fs';

@Injectable()
export class PostService {
    private readonly geocoder: NodeGeocoder.Geocoder = NodeGeocoder({ provider: 'openstreetmap' });

    constructor(
        private readonly prisma: PrismaService,
        private readonly cdn: CdnService
    ) {}


    async createPost(image: Express.Multer.File, post: CreatePostDto, user: User): Promise<ResponseMessage> {
        const { long, lat } = await this.getCoordinates(post.localisation);
        const imageExt: string = image.mimetype.replace('image/', '');
        const cleanTags: string[] = (post.tags ?? []).map(t => t.toLowerCase().trim());

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

            await this.prisma.user.update({
                where: { id: user.id },
                data: { nbPosts: { increment: 1 } }
            });

            const imagePath: string = newPost.id + '.' + imageExt;
            const postUrl = this.cdn.getPostPath(imagePath);

            await sharp(image.buffer).toFile(postUrl);
        } catch (error) {
            throw new BadRequestException('Error during post creation');
        }

        return { status: true, message: 'New post created' };
    }



    async getPost(image: string): Promise<PostInfos> {
        const post = await this.prisma.post.findUniqueOrThrow({
            where: { id: image },
            include: {
                Group: true,
                postTags: { include: { tag: true } }
            }
        });

        return {
            image: this.cdn.getPostUrl(post.id, post.imageExt),
            date: post.date,
            localisation: post.localisation,
            long: post.long,
            lat: post.lat,
            description: post.description,
            nbLikes: post.nbLikes,
            nbComments: post.nbComments,
            groupName: post.Group?.name,
            groupAvatar: post.Group?.avatar,
            tags: post.postTags.map(pt => pt.tag.name)
        };
    }



    async deletePost(image: string, user: User): Promise<ResponseMessage> {
        const post: Post | null = await this.prisma.post.delete({
            where: {
                id: image,
                userId: user.id
            }
        });

        if (!post) {
            throw new BadRequestException('Post does not exist');
        }

        await this.prisma.user.update({
            where: { id: user.id },
            data: { nbPosts: { decrement: 1 } }
        });

        const imageName = image + '.' + post.imageExt;
        const imagePath = this.cdn.getPostPath(imageName);
        await fs.promises.unlink(imagePath);

        return { status: true, message: 'Post deleted' };
    }



    private async getCoordinates(address: string): Promise<{ long: number, lat: number }> {
        const res = await this.geocoder.geocode(address);

        if (!res.length || !res[0].latitude || !res[0].longitude) {
            throw new BadRequestException('Localisation is not correct');
        }

        return { long: res[0].longitude, lat: res[0].latitude };
    }
}
