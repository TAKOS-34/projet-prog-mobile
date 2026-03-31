import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import type { PostDto } from './dto/post.dto';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { Post, User } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';
import NodeGeocoder from 'node-geocoder';
import sharp from 'sharp';
import { CdnService } from 'src/cdn/cdn.service';
import { PostInfos } from './dto/postInfos.dto';

@Injectable()
export class PostService {
    private readonly geocoder: NodeGeocoder.Geocoder = NodeGeocoder({ provider: 'openstreetmap' });

    constructor(
        private readonly prisma: PrismaService,
        private readonly cdnService: CdnService
    ) {}


    async createPost(image: Express.Multer.File, post: PostDto, user: User): Promise<ResponseMessage> {
        const { long, lat } = await this.getCoordinates(post.localisation);
        const imageExt: string = image.mimetype.replace('image/', '');

        const newPost: Post = await this.prisma.post.create({ data: {
            imageExt,
            lat,
            long,
            userId: user.id,
            ...post,
        }});

        const imagePath: string = newPost.id + '.' + imageExt;
        const postUrl = this.cdnService.getPostPath(imagePath);

        await sharp(image.buffer)
            .toFile(postUrl);

        return { status: true, message: 'New post accepted' };
    }



    async getPost(image: string): Promise<PostInfos> {
        const post = await this.prisma.post.findFirst({
            where: { id: image },
            include: { Group: true }
        });

        if (!post) {
            throw new NotFoundException('Invalid post');
        }

        return {
            image: this.cdnService.getPostUrl(post.id, post.imageExt),
            date: post.date,
            localisation: post.localisation,
            long: post.long,
            lat: post.lat,
            description: post.description ?? undefined,
            nbLikes: post.nbLikes,
            nbComments: post.nbComments,
            groupName: post.Group?.name,
            groupAvatar: post.Group?.avatar ?? undefined
        };
    }



    private async getCoordinates(address: string): Promise<{ long: number, lat: number }> {
        const res = await this.geocoder.geocode(address);

        if (!res.length || !res[0].latitude || !res[0].longitude) {
            throw new BadRequestException('Localisation is not correct');
        }

        return { long: res[0].longitude, lat: res[0].latitude };
    }
}
