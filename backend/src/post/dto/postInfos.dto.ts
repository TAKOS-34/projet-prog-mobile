export type PostDto = {
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
    minPrice?: number;
    maxPrice?: number;
    minDuration?: number;
    maxDuration?: number;
    nbLikes: number;
    nbComments: number;
    userId: number,
    username: string;
    avatar: string;
    groupId?: number;
    groupName?: string;
    groupAvatar?: string;
    isMember?: boolean;
    tags: string[];
    isLiked: boolean;
    isBookmarked: boolean;
}

export type PostsInfos = {
    posts: PostDto[];
    nextCursor?: string;
}