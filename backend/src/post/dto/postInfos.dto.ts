export type PostInfos = {
    image: string;
    date: Date;
    localisation: string;
    long: number;
    lat: number;
    description?: string;
    nbLikes: number;
    nbComments: number;
    groupName?: string;
    groupAvatar?: string;
}