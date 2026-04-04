import { Body, Controller, Delete, Param, Patch, Post, UseGuards } from '@nestjs/common';
import { CommentService } from './comment.service';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { AuthGuard } from 'src/auth/auth.guard';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import type { User } from '@prisma/client';
import { CreateCommentDto } from './dto/createComment.dto';
import { UpdateCommentDto } from './dto/updateComment.dto';
import { GroupProtectGuard } from 'src/group/group.protect.guard';

@Controller('comment')
export class CommentController {
    constructor(private readonly commentService: CommentService) {}



    @UseGuards(AuthGuard, GroupProtectGuard)
    @Post('/:postId')
    createComment(@Param('postId') postId: string, @Body() createCommentDto: CreateCommentDto, @GetUser() user: User): Promise<ResponseMessage> {
        return this.commentService.createComment(postId, createCommentDto, user);
    }



    @UseGuards(AuthGuard, GroupProtectGuard)
    @Patch('/:commentId')
    updateComment(@Param('commentId') commentId: string, @Body() content: UpdateCommentDto, @GetUser() user: User): Promise<ResponseMessage> {
        return this.commentService.updateComment(+commentId, content, user);
    }



    @UseGuards(AuthGuard, GroupProtectGuard)
    @Delete('/:commentId')
    deleteComment(@Param('commentId') commentId: string, @GetUser() user: User): Promise<ResponseMessage> {
        return this.commentService.deleteComment(+commentId, user);
    }
}
