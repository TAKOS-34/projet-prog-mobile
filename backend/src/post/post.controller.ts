import { Controller, Post, Get, Delete, UseGuards, Body, UseInterceptors, UploadedFile, ParseFilePipe, MaxFileSizeValidator, FileTypeValidator, Param, Patch } from '@nestjs/common';
import { PostCommandService } from './post.command.service';
import { CreatePostDto } from './dto/createPost.dto';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { AuthGuard } from 'src/auth/auth.guard';
import { FileInterceptor } from '@nestjs/platform-express';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import type { User } from '@prisma/client';
import { PostInfos } from './dto/postInfos.dto';
import { PostQueryService } from './post.query.service';
import { GroupProtectGuard } from 'src/group/group.protect.guard';
import { ReportDto } from './dto/report.dto';
import { UpdatePostDto } from './dto/updatePost.dto';

@Controller('post')
export class PostController {
    constructor(
        private readonly postCommandService: PostCommandService,
        private readonly postQueryService: PostQueryService
    ) {}



    @UseGuards(AuthGuard)
    @Post()
    @UseInterceptors(FileInterceptor('image'))
    createPost(@UploadedFile(
        new ParseFilePipe({
            validators: [
                new MaxFileSizeValidator({ maxSize: 5 * 1000 * 1000 }),
                new FileTypeValidator({
                    fileType: /^image\/(png|jpe?g)(;.*)?$/i,
                    fallbackToMimetype: true,
                }),
            ],
        }),
    ) image: Express.Multer.File, @Body() createPostDto: CreatePostDto, @GetUser() user: User): Promise<ResponseMessage> {
        return this.postCommandService.createPost(image, createPostDto, user);
    }



    @UseGuards(GroupProtectGuard)
    @Get('/:postId')
    getPost(@Param('postId') postId: string): Promise<PostInfos> {
        return this.postQueryService.getPost(postId);
    }



    @UseGuards(AuthGuard, GroupProtectGuard)
    @Patch('/:postId')
    updatePost(@Param('postId') postId: string, @Body() updatePostDto: UpdatePostDto, @GetUser() user: User): Promise<ResponseMessage> {
        return this.postCommandService.updatePost(postId, updatePostDto, user);
    }



    @UseGuards(AuthGuard, GroupProtectGuard)
    @Delete('/:postId')
    deletePost(@Param('postId') postId: string, @GetUser() user: User): Promise<ResponseMessage> {
        return this.postCommandService.deletePost(postId, user);
    }



    @UseGuards(AuthGuard, GroupProtectGuard)
    @Post('/report/:postId')
    reportPost(@Param('postId') postId: string, @Body() reportDto: ReportDto, @GetUser() user: User): Promise<ResponseMessage> {
        return this.postCommandService.reportPost(postId, reportDto, user);
    }
}
