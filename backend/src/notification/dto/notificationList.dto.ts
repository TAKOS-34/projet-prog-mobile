export type NotificationList = {
    id: number;
    type: string;
    creationDate: Date;
    isRead: boolean;

    postId?: string;
    postImage?: string;

    targetUserId?: number;
    targetUsername?: string;
    targetUserAvatar?: string;

    groupId?: number;
    groupName?: string;
    groupAvatar?: string;

    tagId?: number;
    tagName?: string;

    localisationId?: number,
    localisationName?: string
}

export type NotificationInfos = {
    notifications: NotificationList[];
    nextCursor?: number;
}