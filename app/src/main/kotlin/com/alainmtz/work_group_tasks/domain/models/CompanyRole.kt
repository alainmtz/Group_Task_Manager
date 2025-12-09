package com.alainmtz.work_group_tasks.domain.models

/**
 * Company member roles with permissions
 */
enum class CompanyRole {
    OWNER,    // Full control, can manage billing
    ADMIN,    // Can manage members and settings
    MEMBER;   // Regular user access

    fun canManageBilling(): Boolean = this == OWNER
    fun canManageMembers(): Boolean = this == OWNER || this == ADMIN
    fun canManageSettings(): Boolean = this == OWNER || this == ADMIN
}
