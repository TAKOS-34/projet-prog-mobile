export type UserGroup = {
    id: number;
    name: string;
    avatar?: string;
    description?: string;
    creationDate: Date;
    isGroupPrivate: boolean;
    nbMembers: number;
    nbPosts: number;
    isAdmin: boolean;
}