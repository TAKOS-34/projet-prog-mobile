import { Get, Body, Ip, Headers, UseGuards, Controller, Post } from '@nestjs/common';
import { AuthService } from './auth.service';
import { CreateUserDto } from './dto/createUser.dto';
import { LoginUserDto } from './dto/loginUser.dto';
import { UAParser } from 'ua-parser-js';
import { AuthGuard } from './auth.guard';
import { GetUser } from 'src/decorator/get-user.decorator';
import type { User } from '@prisma/client';

@Controller('auth')
export class AuthController {
    constructor(private authService: AuthService) {}

    @Post('signup')
    signUp(@Body() signUpUserDto: CreateUserDto) {
        return this.authService.signUp(signUpUserDto);
    }

    @Post('login')
    login(@Body() loginUserDto: LoginUserDto, @Ip() ip: string, @Headers('user-agent') ua: string) {
        const parser = new UAParser(ua);

        return this.authService.login(loginUserDto, ip, parser.getDevice().type ?? 'unknow');
    }

    @UseGuards(AuthGuard)
    @Get('profile')
    getProfile(@GetUser() user: User) {
        return user;
    }
}
