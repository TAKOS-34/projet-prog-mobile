import { Controller, Post, Get, Delete, UseGuards, Body, UseInterceptors, UploadedFile, ParseFilePipe, MaxFileSizeValidator, FileTypeValidator, Param } from '@nestjs/common';
import { PostService } from './post.service';
import { CreatePostDto } from './dto/createPost.dto';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { AuthGuard } from 'src/auth/auth.guard';
import { FileInterceptor } from '@nestjs/platform-express';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import type { User } from '@prisma/client';
import { PostInfos } from './dto/postInfos.dto';

@Controller('post')
export class PostController {
    constructor(private readonly postService: PostService) {}



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
        return this.postService.createPost(image, createPostDto, user);
    }



    @UseGuards(AuthGuard)
    @Get('/:image')
    getPost(@Param('image') image: string): Promise<PostInfos> {
        return this.postService.getPost(image);
    }



    @UseGuards(AuthGuard)
    @Delete('/:image')
    deletePost(@Param('image') image: string, @GetUser() user: User): Promise<ResponseMessage> {
        return this.postService.deletePost(image, user);
    }
}
