import { Controller, Post, Param, Delete, UseGuards } from '@nestjs/common';
import { LikeService } from './like.service';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import type { UserSession } from 'src/utils/dto/userSession.dto';
import { GetAnonymous } from 'src/utils/decorator/get-anonymous.decorator';
import { AuthOptionalGuard } from '../auth/auth.optionnal.guard';
import { GroupProtectGuard } from 'src/group/group.protect.guard';

@Controller('like')
export class LikeController {
    constructor(private readonly likeService: LikeService) {}


    @UseGuards(GroupProtectGuard, AuthOptionalGuard)
    @Post('/post/:postId')
    addLikePost(@Param('postId') postId: string, @GetUser() user: UserSession, @GetAnonymous() anonymous: string): Promise<ResponseMessage> {
        return this.likeService.addLikePost(postId, user, anonymous);
    }


    @UseGuards(GroupProtectGuard, AuthOptionalGuard)
    @Delete('/post/:postId')
    deleteLikePost(@Param('postId') postId: string, @GetUser() user: UserSession, @GetAnonymous() anonymous: string): Promise<ResponseMessage> {
        return this.likeService.deleteLikePost(postId, user, anonymous);
    }



    @UseGuards(GroupProtectGuard, AuthOptionalGuard)
    @Post('/comment/:commentId')
    addLikeComment(@Param('commentId') commentId: number, @GetUser() user: UserSession, @GetAnonymous() anonymous: string): Promise<ResponseMessage> {
        return this.likeService.addLikeComment(commentId, user, anonymous);
    }



    @UseGuards(GroupProtectGuard, AuthOptionalGuard)
    @Delete('/comment/:commentId')
    deleteLikeComment(@Param('commentId') commentId: number, @GetUser() user: UserSession, @GetAnonymous() anonymous: string): Promise<ResponseMessage> {
        return this.likeService.deleteLikeComment(commentId, user, anonymous);
    }
}
