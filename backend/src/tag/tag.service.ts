import { Injectable, InternalServerErrorException } from '@nestjs/common';
import { PrismaService } from 'src/prisma/prisma.service';
import { GenerateContentResponse, GoogleGenAI } from "@google/genai";
import { $Enums } from '@prisma/client';

@Injectable()
export class TagService {
    private readonly gemini: GoogleGenAI = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
    private readonly model: string = 'gemini-2.5-flash-lite';

    constructor(private readonly prisma: PrismaService) {}



    async getPopularTag(): Promise<string[]> {
        const tags = await this.prisma.tag.findMany({
            select: { name: true },
            orderBy: { nbUses: 'desc' },
            take: 10
        });

        return tags.map(t => t.name);
    }



    async suggestTag(tag: string): Promise<string[]> {
        const tags = await this.prisma.tag.findMany({
            where: { name: { contains: tag } },
            select: { name: true },
            orderBy: { nbUses: 'desc' },
            take: 10
        });

        return tags.map(t => t.name);
    }



    async iaSuggestions(post: Express.Multer.File): Promise<string[]> {
        try {
            const response = await this.gemini.models.generateContent({
                model: this.model,
                contents: [
                    { text: 'Analyze this image and generate 3 descriptive tags for Instagram. Return ONLY the tags separated by commas. Do not use hashtags, brackets, or any preamble. Example: paris, france, baguette' }, 
                    { inlineData: {
                        mimeType: post.mimetype,
                        data: post.buffer.toString('base64')
                    }}
                ]
            });

            if (!response.text) {
                throw new InternalServerErrorException('Internal server error');
            }

            return response.text
                .replace(/[\[\]"'`]/g, '')
                .split(',')
                .map(tag => tag.trim())
                .filter(tag => tag.length > 0);
        } catch (error) {
            throw new InternalServerErrorException('Internal server error');
        }
    }
}
