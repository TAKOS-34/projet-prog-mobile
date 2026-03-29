import { Injectable } from '@nestjs/common';
import { MailerService } from '@nestjs-modules/mailer';

@Injectable()
export class AppMailerService {
    private readonly BASE_URL: string = `${process.env.URL}`;

    constructor(private readonly mailerService: MailerService) {}



    async sendConfirmationEmail(email: string, username: string, token: string): Promise<void> {
        try {
            const url: string = `${this.BASE_URL}/auth/confirm-email/${token}`

            await this.mailerService.sendMail({
                to: email,
                subject: 'Confirm your email | 7N',
                template: 'confirmation-email',
                context: { username, url }
            });
        } catch (error) {
            console.error(error);
        }
    }



    async sendUpdatedProfileEmail(email: string, username: string, changes: { emailChanged: boolean, usernameChanged: boolean, passwordChanged: boolean }): Promise<void> {
        try {
            await this.mailerService.sendMail({
                to: email,
                subject: 'Profile updated | 7N',
                template: 'profile-updated-email',
                context: { username, changes }
            });
        } catch (error) {
            console.error(error)
        }
    }



    async sendDeleteAccountEmail(email: string, username: string): Promise<void> {
        try {
            await this.mailerService.sendMail({
                to: email,
                subject: 'Account deleted | 7N',
                template: 'account-deleted-email',
                context: { username }
            });
        } catch (error) {
            console.error(error)
        }
    }
}
