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
            console.error(error);
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
            console.error(error);
        }
    }



    async sendGroupBanEmail(email: string, username: string, groupName: string): Promise<void> {
        try {
            await this.mailerService.sendMail({
                to: email,
                subject: 'Ban from group | 7N',
                template: 'group-ban',
                context: { username, groupName }
            });
        } catch (error) {
            console.error(error);
        }
    }



    async sendTransferAdminRoleEmail(email: string, username: string, groupName: string, newUser: string): Promise<void> {
        try {
            await this.mailerService.sendMail({
                to: email,
                subject: 'You transferred admin role | 7N',
                template: 'transfer-admin-role',
                context: { username, groupName, newUser }
            });
        } catch (error) {
            console.error(error);
        }
    }



    async sendReceiveAdminRoleEmail(email: string, username: string, groupName: string, oldAdmin: string): Promise<void> {
        try {
            await this.mailerService.sendMail({
                to: email,
                subject: 'You receive admin role | 7N',
                template: 'receive-admin-role',
                context: { username, groupName, oldAdmin }
            });
        } catch (error) {
            console.error(error);
        }
    }
}
