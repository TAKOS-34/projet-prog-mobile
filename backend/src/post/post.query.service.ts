import { Injectable } from "@nestjs/common";
import { CdnService } from "src/cdn/cdn.service";
import { PrismaService } from "src/prisma/prisma.service";
import { PostInfos } from "./dto/postInfos.dto";

@Injectable()
export class PostQueryService {
    constructor(
        private readonly prisma: PrismaService,
        private readonly cdn: CdnService
    ) {}



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
            groupAvatar: post.Group?.avatar ? this.cdn.getGroupAvatarUrl(post.Group.avatar) : undefined,
            tags: post.postTags.map(pt => pt.tag.name)
        };
    }
}