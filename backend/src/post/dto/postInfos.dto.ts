export type PostInfos = {
    id: string;
    image: string;
    creationDate: Date;
    isEdited: boolean;
    updatedAt: Date | null;
    localisation: string;
    long: number;
    lat: number;
    title: string;
    description: string | null;
    audio: string | null;
    nbLikes: number;
    nbComments: number;
    groupName: string | null;
    groupAvatar: string | null;
    tags: string[];
}