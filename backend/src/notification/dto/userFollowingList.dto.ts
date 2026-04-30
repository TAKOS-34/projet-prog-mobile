export type UserFollowingList = {
    type: string,

    targetUserId?: number,
    targetUsername?: string,
    targetUserAvatar?: string,

    targetGroupId?: number,
    targetGroupName?: string,
    targetGroupAvatar?: string,

    targetTagId?: number,
    targetTagName?: string,

    targetLocalisationId?: number,
    targetLocalisationName?: string
}