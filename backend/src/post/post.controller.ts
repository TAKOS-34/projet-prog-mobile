import { Controller, Post, Get, Delete, UseGuards, Body, UseInterceptors, UploadedFile, ParseFilePipe, MaxFileSizeValidator, FileTypeValidator, Param, Patch, UploadedFiles, Query } from '@nestjs/common';
import { PostCommandService } from './post.command.service';
import { CreatePostDto } from './dto/createPost.dto';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { AuthGuard } from 'src/auth/auth.guard';
import { FileFieldsInterceptor } from '@nestjs/platform-express';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import { PostInfos } from './dto/postInfos.dto';
import { PostQueryService } from './post.query.service';
import { GroupProtectGuard } from 'src/group/group.protect.guard';
import { ReportDto } from './dto/report.dto';
import { UpdatePostDto } from './dto/updatePost.dto';
import { PostFilesValidatorPipe } from './pipe/post-files-validator.pipe';
import { AuthOptionalGuard } from 'src/auth/auth.optionnal.guard';
import { GetAnonymous } from 'src/utils/decorator/get-anonymous.decorator';
import { CommentInfos } from './dto/comment.dto';
import type { UserSession } from 'src/utils/dto/userSession.dto';

@Controller('post')
export class PostController {
    constructor(
        private readonly postCommandService: PostCommandService,
        private readonly postQueryService: PostQueryService
    ) {}


    @Get()
    @UseGuards(AuthOptionalGuard)
    async getGlobalFeed(@Query('limit') limit: string = '20', @Query('cursor') cursor?: string, @GetUser() user?: UserSession, @GetAnonymous() anonymous?: string): Promise<PostInfos[]> {
        return this.postQueryService.getFeed(parseInt(limit), cursor, user?.id, anonymous);
    }

    @UseGuards(GroupProtectGuard, AuthOptionalGuard)
    @Get('/:postId')
    getPost(@Param('postId') postId: string, @GetUser() user?: UserSession, @GetAnonymous() anonymous?: string): Promise<PostInfos> {
        return this.postQueryService.getPost(postId, user?.id, anonymous);
    }

    @UseGuards(GroupProtectGuard, AuthOptionalGuard)
    @Get('/:postId/comments')
    getPostComments(@Param('postId') postId: string, @GetUser() user?: UserSession, @GetAnonymous() anonymous?: string): Promise<CommentInfos[]> {
        return this.postQueryService.getPostComments(postId, user?.id, anonymous);
    }



    @UseGuards(AuthGuard)
    @Post()
    @UseInterceptors(FileFieldsInterceptor([
        { name: 'image', maxCount: 1 },
        { name: 'audio', maxCount: 1 },
    ]))
    createPost(
        @UploadedFiles(new PostFilesValidatorPipe()) files: { image: Express.Multer.File, audio?: Express.Multer.File },
        @Body() createPostDto: CreatePostDto,
        @GetUser() user: UserSession
    ): Promise<ResponseMessage> {
        return this.postCommandService.createPost(files.image, createPostDto, user, files.audio);
    }



    @UseGuards(AuthGuard, GroupProtectGuard)
    @Patch('/:postId')
    updatePost(@Param('postId') postId: string, @Body() updatePostDto: UpdatePostDto, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.postCommandService.updatePost(postId, updatePostDto, user);
    }



    @UseGuards(AuthGuard, GroupProtectGuard)
    @Delete('/:postId')
    deletePost(@Param('postId') postId: string, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.postCommandService.deletePost(postId, user);
    }



    @UseGuards(AuthGuard, GroupProtectGuard)
    @Post('/report/:postId')
    reportPost(@Param('postId') postId: string, @Body() reportDto: ReportDto, @GetUser() user: UserSession): Promise<ResponseMessage> {
        return this.postCommandService.reportPost(postId, reportDto, user);
    }
}
