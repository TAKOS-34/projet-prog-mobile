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



    async getPost(postId: string): Promise<PostInfos> {
        const post = await this.prisma.post.findUniqueOrThrow({
            where: { id: postId },
            include: {
                Group: true,
                postTags: { include: { tag: true } }
            }
        });

        return {
            id: post.id,
            image: this.cdn.getPostUrl(post.id, post.imageExt),
            creationDate: post.creationDate,
            isEdited: post.isEdited,
            updatedAt: post.updatedAt,
            title: post.title,
            localisation: post.localisation,
            long: post.long,
            lat: post.lat,
            description: post.description,
            nbLikes: post.nbLikes,
            nbComments: post.nbComments,
            groupName: post.Group?.name ?? null,
            groupAvatar: post.Group?.avatar ? this.cdn.getGroupAvatarUrl(post.Group.avatar) : null,
            tags: post.postTags.map(pt => pt.tag.name)
        };
    }
}