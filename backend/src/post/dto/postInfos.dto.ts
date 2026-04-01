export type PostInfos = {
    image: string;
    date: Date;
    localisation: string;
    long: number;
    lat: number;
    description?: string | null;
    nbLikes: number;
    nbComments: number;
    groupName?: string;
    groupAvatar?: string | null;
    tags: string[];
}