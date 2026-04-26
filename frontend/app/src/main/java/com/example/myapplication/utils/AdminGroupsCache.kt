package com.example.myapplication.utils

object AdminGroupsCache {
    private val adminGroupIds = mutableSetOf<Int>()

    fun set(ids: Collection<Int>) {
        adminGroupIds.clear()
        adminGroupIds.addAll(ids)
    }

    fun isAdminOf(groupId: Int?): Boolean = groupId != null && adminGroupIds.contains(groupId)

    fun clear() = adminGroupIds.clear()
}
