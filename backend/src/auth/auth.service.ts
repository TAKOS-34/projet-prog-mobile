import { BadRequestException, Injectable, UnauthorizedException } from '@nestjs/common';
import { CreateUserDto } from './dto/createUser.dto';
import { PrismaService } from 'src/prisma/prisma.service';
import { AppMailerService } from 'src/mailer/mailer.service';
import { LoginUserDto } from './dto/loginUser.dto';
import { User, UserToken } from '@prisma/client';
import * as bcrypt from 'bcrypt';
import { createHash, randomBytes } from 'crypto';
import { ResponseMessage, TokenResponseMessage } from 'src/utils/dto/responseMessage.dto';

@Injectable()
export class AuthService {
    private saltRounds: number = Number(process.env.BCRYPT_SALT) ?? 10;

    constructor(
        private readonly prisma: PrismaService,
        private readonly mailer: AppMailerService
    ) {}



    async signUp(user: CreateUserDto): Promise<ResponseMessage> {
        const existingUser: User | null = await this.prisma.user.findFirst({
            where: { OR : [{ email: user.email }, { username: user.username }] }
        });

        if (existingUser) {
            throw new BadRequestException('Email or username already taken');
        }

        const hashedPassword: string = await bcrypt.hash(user.password, this.saltRounds);
        const token: string = randomBytes(64).toString('hex');
        const hashedTokenVerification: string = this.hashToken(token);

        await this.prisma.user.create({ data: {
            email: user.email,
            username: user.username,
            password: hashedPassword,
            emailVerificationToken: hashedTokenVerification
        }});

        await this.mailer.sendConfirmationEmail(user.email, user.username, token);

        return { status: true, message: 'Please verify your email' };
    }



    async login(user: LoginUserDto, ip: string, device: string): Promise<TokenResponseMessage> {
        const existingUser: User | null = await this.prisma.user.findUnique({
            where: { username: user.username }
        });

        if (!existingUser || !(await bcrypt.compare(user.password, existingUser.password))) {
            throw new BadRequestException('Username or password incorrect');
        }

        if (!existingUser.isEmailVerified) {
            throw new UnauthorizedException('Please verify your email before login');
        }

        await this.prisma.user.update({
            where: { id: existingUser.id },
            data: { lastTimeLogin: new Date() }
        });

        const expirationDate: Date = new Date();
        expirationDate.setDate(expirationDate.getDate() + 30);
        const realDevice: string = device ? device : 'unknow';

        const token: UserToken = await this.prisma.userToken.create({ data: {
            expirationDate: expirationDate,
            ip,
            device: realDevice,
            userId: existingUser.id,
        }});

        return { status: true,  token: `Bearer ${token.id}` };
    }



    async confirmEmail(token: string): Promise<string> {
        const hashedToken: string = this.hashToken(token);

        try {
            await this.prisma.user.update({
                where: { emailVerificationToken: hashedToken},
                data: { isEmailVerified: true, emailVerificationToken: null }
            });

            return 'Email verified, you can login';
        } catch (error) {
            return 'Invalid token';
        }
    }



    private hashToken(token: string): string {
        return createHash('sha256').update(token).digest('hex');
    }
}
