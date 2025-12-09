package com.alainmtz.work_group_tasks.domain.models

/**
 * Default plan configurations - Defines all 4 subscription tiers
 */
object PlanDefaults {
    
    val FREE = Plan(
        id = "free",
        name = "Free Plan",
        tier = PlanTier.FREE,
        priceMonthly = 0.0,
        priceYearly = 0.0,
        features = PlanFeatures(
            maxGroups = 1,
            maxMembersPerGroup = 5,
            maxActiveTasks = 10,
            maxStorageGB = 0,
            maxStorageMB = 100,
            maxPhotosPerMonth = 5,
            canUseBudgets = false,
            canUseBidding = false,
            canUseApprovalHierarchy = false,
            canUseMultimediaChat = false,
            canExportData = false,
            canUseAnalytics = false,
            canUseAuditLogs = false,
            canCustomizeNotifications = false,
            supportLevel = "standard",
            canUseSSO = false,
            canUseAPI = false,
            canWhiteLabel = false
        )
    )
    
    val PRO = Plan(
        id = "pro",
        name = "Pro Plan",
        tier = PlanTier.PRO,
        priceMonthly = 6.99,
        priceYearly = 69.0,
        features = PlanFeatures(
            maxGroups = -1, // Unlimited
            maxMembersPerGroup = 15,
            maxActiveTasks = 50,
            maxStorageGB = 5,
            maxStorageMB = 0,
            maxPhotosPerMonth = -1, // Unlimited
            canUseBudgets = true,
            canUseBidding = true,
            canUseApprovalHierarchy = false,
            canUseMultimediaChat = true,
            canExportData = false,
            canUseAnalytics = true,
            canUseAuditLogs = false,
            canCustomizeNotifications = false,
            supportLevel = "priority",
            canUseSSO = false,
            canUseAPI = false,
            canWhiteLabel = false
        )
    )
    
    val BUSINESS = Plan(
        id = "business",
        name = "Business Plan",
        tier = PlanTier.BUSINESS,
        priceMonthly = 39.0,
        priceYearly = 399.0,
        features = PlanFeatures(
            maxGroups = -1, // Unlimited
            maxMembersPerGroup = 50,
            maxActiveTasks = 200,
            maxStorageGB = 50,
            maxStorageMB = 0,
            maxPhotosPerMonth = -1, // Unlimited
            canUseBudgets = true,
            canUseBidding = true,
            canUseApprovalHierarchy = true,
            canUseMultimediaChat = true,
            canExportData = true,
            canUseAnalytics = true,
            canUseAuditLogs = true,
            canCustomizeNotifications = true,
            supportLevel = "dedicated",
            canUseSSO = false,
            canUseAPI = false,
            canWhiteLabel = false
        )
    )
    
    val ENTERPRISE = Plan(
        id = "enterprise",
        name = "Enterprise Plan",
        tier = PlanTier.ENTERPRISE,
        priceMonthly = 0.0, // Custom pricing
        priceYearly = 0.0,  // Custom pricing
        features = PlanFeatures(
            maxGroups = -1, // Unlimited
            maxMembersPerGroup = -1, // Unlimited
            maxActiveTasks = -1, // Unlimited
            maxStorageGB = -1, // Unlimited
            maxStorageMB = 0,
            maxPhotosPerMonth = -1, // Unlimited
            canUseBudgets = true,
            canUseBidding = true,
            canUseApprovalHierarchy = true,
            canUseMultimediaChat = true,
            canExportData = true,
            canUseAnalytics = true,
            canUseAuditLogs = true,
            canCustomizeNotifications = true,
            supportLevel = "dedicated_manager",
            canUseSSO = true,
            canUseAPI = true,
            canWhiteLabel = true
        )
    )
    
    /**
     * Get plan by ID
     */
    fun getPlanById(id: String): Plan {
        return when (id.lowercase()) {
            "free" -> FREE
            "pro" -> PRO
            "business" -> BUSINESS
            "enterprise" -> ENTERPRISE
            else -> FREE
        }
    }
    
    /**
     * Get plan by tier
     */
    fun getPlanByTier(tier: PlanTier): Plan {
        return when (tier) {
            PlanTier.FREE -> FREE
            PlanTier.PRO -> PRO
            PlanTier.BUSINESS -> BUSINESS
            PlanTier.ENTERPRISE -> ENTERPRISE
        }
    }
    
    /**
     * Get all plans
     */
    fun getAllPlans(): List<Plan> {
        return listOf(FREE, PRO, BUSINESS, ENTERPRISE)
    }
}
