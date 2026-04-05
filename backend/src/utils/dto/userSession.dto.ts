import type { User } from "@prisma/client";

export type UserSession = Pick<
    User,
    'id' | 'email' | 'username' | 'creationDate' | 'avatar' | 'nbPosts' | 'nbGroups' | 'isEmailVerified'
>;