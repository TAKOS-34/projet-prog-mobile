import { Controller, Post, Get, Delete, UseGuards, Body, UseInterceptors, UploadedFile, ParseFilePipe, MaxFileSizeValidator, FileTypeValidator, Param } from '@nestjs/common';
import { PostCommandService } from './post.command.service';
import { CreatePostDto } from './dto/createPost.dto';
import { ResponseMessage } from 'src/utils/dto/responseMessage.dto';
import { AuthGuard } from 'src/auth/auth.guard';
import { FileInterceptor } from '@nestjs/platform-express';
import { GetUser } from 'src/utils/decorator/get-user.decorator';
import type { User } from '@prisma/client';
import { PostInfos } from './dto/postInfos.dto';
import { PostQueryService } from './post.query.service';

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



    @Get('/:image')
    getPost(@Param('image') image: string): Promise<PostInfos> {
        return this.postQueryService.getPost(image);
    }



    @UseGuards(AuthGuard)
    @Delete('/:image')
    deletePost(@Param('image') image: string, @GetUser() user: User): Promise<ResponseMessage> {
        return this.postCommandService.deletePost(image, user);
    }
}
