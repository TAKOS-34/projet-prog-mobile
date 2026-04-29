export type PostInfos = {
    id: string;
    image: string;
    creationDate: Date;
    isEdited: boolean;
    updatedAt?: Date;
    localisation: string;
    long: number;
    lat: number;
    title: string;
    description?: string;
    type: string;
    audio?: string;
    audioDuration?: number;
    nbLikes: number;
    nbComments: number;
    userId: number,
    username: string;
    avatar: string;
    groupId?: number;
    groupName?: string;
    groupAvatar?: string;
    tags: string[];
    isLiked: boolean;
}

export type FeedInfos = {
    posts: PostInfos[];
    nextCursor?: string;
}