import { BadRequestException, HttpException, Injectable } from '@nestjs/common';
import { CreateUserDto } from './dto/createUser.dto';
import { PrismaService } from 'src/prisma.service';
import { LoginUserDto } from './dto/loginUser.dto';
import { User, UserToken } from '@prisma/client';
import * as bcrypt from 'bcrypt';

@Injectable()
export class AuthService {
    constructor(private readonly prisma: PrismaService) {}

    async signUp(user: CreateUserDto): Promise<User> {
        const existingUser: User | null = await this.prisma.user.findUnique({
            where: { email: user.email }
        });

        if (existingUser) {
            throw new BadRequestException('Email already taken');
        }

        const saltRounds = parseInt(process.env.BCRYPT_SALT || '10', 10);
        const hashedPassword = await bcrypt.hash(user.password, saltRounds);

        return this.prisma.user.create({ data: {
            email: user.email,
            username: user.username,
            password: hashedPassword,
        }});
    }

    async login(user: LoginUserDto, ip: string, device: string): Promise<string> {
        const realUser: User | null = await this.prisma.user.findUnique({
            where: { email: user.email }
        });

        if (!realUser || !(await bcrypt.compare(user.password, realUser.password))) {
            throw new BadRequestException('Email or password incorrect');
        }

        await this.prisma.user.update({
            where: { id: realUser.id },
            data: { lastTimeLogin: new Date() }
        });

        const expirationDate: Date = new Date();
        expirationDate.setDate(expirationDate.getDate() + 30);

        const token: UserToken = await this.prisma.userToken.create({ data: {
            exprirationDate: expirationDate,
            ip,
            device,
            userId: realUser.id,
        }});

        return token.id;
    }
}
