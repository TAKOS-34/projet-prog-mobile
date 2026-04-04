import { Injectable } from '@nestjs/common';
import { PrismaService } from 'src/prisma/prisma.service';
import { CreateCommentDto } from './dto/createComment.dto';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import type { User } from '@prisma/client';
import { UpdateCommentDto } from './dto/updateComment.dto';

@Injectable()
export class CommentService {
    constructor(private readonly prisma: PrismaService) {}



    async createComment(postId: string, comment: CreateCommentDto, user: User): Promise<ResponseMessage> {
        await this.prisma.comment.create({ data: {
            content: comment.content,
            postId,
            userId: user.id,
            parentId: comment.parentId ?? null
        }});

        return { status: true, message: 'Comment created' };
    }



    async updateComment(commentId: number, comment: UpdateCommentDto, user: User): Promise<ResponseMessage> {
        const { content } = comment;

        await this.prisma.comment.update({
            data: { content, updatedAt: new Date(), isEdited: true },
            where: { id: commentId, userId: user.id }
        });

        return { status: true, message: 'Comment updated' };
    }



    async deleteComment(commentId: number, user: User): Promise<ResponseMessage> {
        await this.prisma.comment.delete({
            where: { id: commentId, userId: user.id }
        });

        return { status: true, message: 'Comment deleted' };
    }
}
