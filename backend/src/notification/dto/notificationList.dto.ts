export type NotificationList = {
    id: number;
    type: string;
    creationDate: Date;
    isRead: boolean;

    senderId?: number;
    senderName?: string;
    senderAvatar?: string;

    postId?: string;
    postImage?: string;
    groupId?: number;
    groupName?: string;
    groupAvatar?: string;
    tagName?: string;
}