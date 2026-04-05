export type GroupInfos = {
    id: number,
    name: string,
    avatar: string,
    description?: string,
    creationDate: Date,
    isGroupPrivate: boolean,
    nbMembers: number,
    nbPosts: number,
    isMember: boolean,
    isAdmin: boolean
}