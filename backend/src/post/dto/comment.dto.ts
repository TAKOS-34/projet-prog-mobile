export type CommentInfos = {
    id: number;
    content: string;
    creationDate: Date;
    updatedAt?: Date;
    isEdited: boolean;
    nbLikes: number;
    nbReplies: number;
    userId: number;
    username: string;
    avatar: string;
    parentId?: number;
}