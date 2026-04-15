export type NotificationList = {
    id: number;
    type: string;
    creationDate: Date;
    isRead: boolean;

    postId?: string;
    postImage?: string;
    postUserId?: number;
    postUsername?: string;
    postUserAvatar?: string;

    groupId?: number;
    groupName?: string;
    groupAvatar?: string;

    tagId?: number;
    tagName?: string;
}