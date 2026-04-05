export type PostSearch = {
    id: string;
    image: string;
    creationDate: Date;
    title: string;
    nbLikes: number;
    nbComments: number;
    userId: number;
    username: string;
    avatar: string;
    isLiked: boolean;
    groupId?: number;
    groupName?: string;
    groupAvatar?: string;
}