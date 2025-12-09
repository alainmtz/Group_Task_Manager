package com.alainmtz.work_group_tasks.domain.services

import com.alainmtz.work_group_tasks.domain.models.Company
import com.alainmtz.work_group_tasks.domain.models.Plan
import com.alainmtz.work_group_tasks.domain.models.PlanTier

/**
 * Feature flag system - Runtime feature checking based on company plan
 */
object FeatureFlags {
    
    /**
     * Check if company can create more groups
     */
    fun canCreateGroup(company: Company, plan: Plan): Pair<Boolean, String?> {
        if (plan.features.maxGroups == -1) return Pair(true, null) // Unlimited
        
        val canCreate = company.groupsCount < plan.features.maxGroups
        val message = if (!canCreate) {
            "Group limit reached (${plan.features.maxGroups}). Upgrade to ${getUpgradeTier(plan.tier)} for more groups."
        } else null
        
        return Pair(canCreate, message)
    }
    
    /**
     * Check if company can create more tasks
     */
    fun canCreateTask(company: Company, plan: Plan): Pair<Boolean, String?> {
        if (plan.features.maxActiveTasks == -1) return Pair(true, null) // Unlimited
        
        val canCreate = company.activeTasksCount < plan.features.maxActiveTasks
        val message = if (!canCreate) {
            "Active task limit reached (${plan.features.maxActiveTasks}). Upgrade to ${getUpgradeTier(plan.tier)} for more tasks."
        } else null
        
        return Pair(canCreate, message)
    }
    
    /**
     * Check if group can add more members
     */
    fun canAddMember(currentMemberCount: Int, plan: Plan): Pair<Boolean, String?> {
        if (plan.features.maxMembersPerGroup == -1) return Pair(true, null) // Unlimited
        
        val canAdd = currentMemberCount < plan.features.maxMembersPerGroup
        val message = if (!canAdd) {
            "Member limit reached (${plan.features.maxMembersPerGroup}). Upgrade to ${getUpgradeTier(plan.tier)} for more members."
        } else null
        
        return Pair(canAdd, message)
    }
    
    /**
     * Check if user can upload more photos this month
     */
    fun canUploadPhoto(company: Company, plan: Plan): Pair<Boolean, String?> {
        if (plan.features.maxPhotosPerMonth == -1) return Pair(true, null) // Unlimited
        
        val canUpload = company.photosUploadedThisMonth < plan.features.maxPhotosPerMonth
        val message = if (!canUpload) {
            "Monthly photo limit reached (${plan.features.maxPhotosPerMonth}). Upgrade to ${getUpgradeTier(plan.tier)} for unlimited uploads."
        } else null
        
        return Pair(canUpload, message)
    }
    
    /**
     * Check storage quota
     */
    fun canUploadFile(company: Company, plan: Plan, fileSizeBytes: Long): Pair<Boolean, String?> {
        val storageLimit = plan.features.getStorageLimitBytes()
        val willExceed = company.storageUsedBytes + fileSizeBytes > storageLimit
        
        val message = if (willExceed) {
            val usedMB = company.storageUsedBytes / (1024 * 1024)
            val limitMB = storageLimit / (1024 * 1024)
            "Storage limit exceeded ($usedMB MB / $limitMB MB). Upgrade to ${getUpgradeTier(plan.tier)} for more storage."
        } else null
        
        return Pair(!willExceed, message)
    }
    
    /**
     * Check if budgeting/bidding features are available
     */
    fun canUseBudgets(plan: Plan): Pair<Boolean, String?> {
        val message = if (!plan.features.canUseBudgets) {
            "Budgeting and bidding require PRO plan or higher."
        } else null
        
        return Pair(plan.features.canUseBudgets, message)
    }
    
    /**
     * Check if approval hierarchy is available
     */
    fun canUseApprovalHierarchy(plan: Plan): Pair<Boolean, String?> {
        val message = if (!plan.features.canUseApprovalHierarchy) {
            "Approval hierarchy requires BUSINESS plan or higher."
        } else null
        
        return Pair(plan.features.canUseApprovalHierarchy, message)
    }
    
    /**
     * Check if multimedia chat is available
     */
    fun canSendMultimedia(plan: Plan): Pair<Boolean, String?> {
        val message = if (!plan.features.canUseMultimediaChat) {
            "Multimedia messages require PRO plan or higher."
        } else null
        
        return Pair(plan.features.canUseMultimediaChat, message)
    }
    
    /**
     * Check if analytics are available
     */
    fun canUseAnalytics(plan: Plan): Pair<Boolean, String?> {
        val message = if (!plan.features.canUseAnalytics) {
            "Analytics require PRO plan or higher."
        } else null
        
        return Pair(plan.features.canUseAnalytics, message)
    }
    
    /**
     * Check if data export is available
     */
    fun canExportData(plan: Plan): Pair<Boolean, String?> {
        val message = if (!plan.features.canExportData) {
            "Data export requires BUSINESS plan or higher."
        } else null
        
        return Pair(plan.features.canExportData, message)
    }
    
    /**
     * Get suggested upgrade tier
     */
    private fun getUpgradeTier(currentTier: PlanTier): String {
        return when (currentTier) {
            PlanTier.FREE -> "PRO"
            PlanTier.PRO -> "BUSINESS"
            PlanTier.BUSINESS -> "ENTERPRISE"
            PlanTier.ENTERPRISE -> "ENTERPRISE" // Already max
        }
    }
    
    /**
     * Get storage usage percentage
     */
    fun getStorageUsagePercentage(company: Company, plan: Plan): Int {
        val limit = plan.features.getStorageLimitBytes()
        if (limit == 0L) return 0
        return ((company.storageUsedBytes.toDouble() / limit) * 100).toInt().coerceIn(0, 100)
    }
    
    /**
     * Get photo usage percentage
     */
    fun getPhotoUsagePercentage(company: Company, plan: Plan): Int {
        val limit = plan.features.maxPhotosPerMonth
        if (limit <= 0 || limit == -1) return 0
        return ((company.photosUploadedThisMonth.toDouble() / limit) * 100).toInt().coerceIn(0, 100)
    }
}
