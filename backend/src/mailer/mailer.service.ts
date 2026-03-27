import { Injectable } from '@nestjs/common';
import { MailerService } from '@nestjs-modules/mailer';

@Injectable()
export class AppMailerService {
    constructor(private readonly mailerService: MailerService) {}



    async sendConfirmationEmail(email: string, username: string, token: string) {
        await this.mailerService.sendMail({
            to: [email],
            subject: `Confirm your email | 7N`,
            html:`
                <div style="font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <h1 style="color: #333; font-size: 20px;">Dear ${username},</h1>
                    
                    <p style="color: #555; line-height: 1.5;">
                        Please confirm your email address by clicking the button below:
                    </p>

                    <div style="margin: 25px 0;">
                        <a href="${process.env.URL}/auth/confirm-email/${token}" 
                        style="background-color: #007bff; color: white; padding: 12px 20px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;">
                            Confirm Email
                        </a>
                    </div>

                    <p style="color: #777; font-size: 14px;">
                        If the button above doesn't work, click this <strong><a href="${process.env.URL}/auth/confirm-email/${token}" style="color: #007bff;">link</a></strong>.
                    </p>

                    <p style="margin-top: 30px; font-weight: bold; color: #333;">
                        Have a great day!
                    </p>
                </div>`
        });
    }
}
