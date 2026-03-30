import { Get, Body, Ip, Headers, Controller, Post, Param, HttpCode } from '@nestjs/common';
import { AuthService } from './auth.service';
import { CreateUserDto } from './dto/createUser.dto';
import { LoginUserDto } from './dto/loginUser.dto';
import { UAParser } from 'ua-parser-js';
import { ResponseMessage, TokenResponseMessage } from 'src/utils/dto/responseMessage.dto';

@Controller('auth')
export class AuthController {
    constructor(private authService: AuthService) {}

    @Post('signup')
    signUp(@Body() signUpUserDto: CreateUserDto): Promise<ResponseMessage> {
        return this.authService.signUp(signUpUserDto);
    }



    @Post('login')
    @HttpCode(200)
    login(@Body() loginUserDto: LoginUserDto, @Ip() ip: string, @Headers('user-agent') ua: string): Promise<TokenResponseMessage> {
        const parser = new UAParser(ua);

        return this.authService.login(loginUserDto, ip, parser.getDevice().type ?? '');
    }



    @Get('confirm-email/:token')
    confirmEmail(@Param('token') token: string): Promise<string> {
        return this.authService.confirmEmail(token);
    }
}
