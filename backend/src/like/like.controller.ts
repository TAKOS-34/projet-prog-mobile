import { Controller, Post, Param, Delete, UseGuards } from '@nestjs/common';
import { LikeService } from './like.service';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import type { User } from '@prisma/client';
import { GetAnonymous } from 'src/utils/decorator/get-anonymous.decorator';
import { AuthLikeGuard } from './auth.like.guard';
import { GroupProtectGuard } from 'src/group/group.protect.guard';

@Controller('like')
export class LikeController {
    constructor(private readonly likeService: LikeService) {}


    @UseGuards(GroupProtectGuard, AuthLikeGuard)
    @Post('/post/:postId')
    addLikePost(@Param('postId') postId: string, @GetUser() user: User, @GetAnonymous() anonymous: string): Promise<ResponseMessage> {
        return this.likeService.addLikePost(postId, user, anonymous);
    }


    @UseGuards(GroupProtectGuard, AuthLikeGuard)
    @Delete('/post/:postId')
    deleteLikePost(@Param('postId') postId: string, @GetUser() user: User, @GetAnonymous() anonymous: string): Promise<ResponseMessage> {
        return this.likeService.deleteLikePost(postId, user, anonymous);
    }



    @UseGuards(GroupProtectGuard, AuthLikeGuard)
    @Post('/comment/:commentId')
    addLikeComment(@Param('commentId') commentId: number, @GetUser() user: User, @GetAnonymous() anonymous: string): Promise<ResponseMessage> {
        return this.likeService.addLikeComment(commentId, user, anonymous);
    }



    @UseGuards(GroupProtectGuard, AuthLikeGuard)
    @Delete('/comment/:commentId')
    deleteLikeComment(@Param('commentId') commentId: number, @GetUser() user: User, @GetAnonymous() anonymous: string): Promise<ResponseMessage> {
        return this.likeService.deleteLikeComment(commentId, user, anonymous);
    }
}
