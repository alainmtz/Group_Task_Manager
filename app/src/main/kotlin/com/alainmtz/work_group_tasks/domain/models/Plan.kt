package com.alainmtz.work_group_tasks.domain.models

import com.google.firebase.firestore.DocumentSnapshot

/**
 * Subscription Plan Tiers
 */
enum class PlanTier {
    FREE,
    PRO,
    BUSINESS,
    ENTERPRISE
}

/**
 * Plan data model - Defines features and limits for each subscription tier
 */
data class Plan(
    val id: String = "",
    val name: String = "",
    val tier: PlanTier = PlanTier.FREE,
    val priceMonthly: Double = 0.0,
    val priceYearly: Double = 0.0,
    val features: PlanFeatures = PlanFeatures()
) {
    companion object {
        fun fromFirestore(doc: DocumentSnapshot): Plan {
            val data = doc.data ?: return Plan()
            return Plan(
                id = doc.id,
                name = data["name"] as? String ?: "",
                tier = try {
                    PlanTier.valueOf((data["tier"] as? String ?: "FREE").uppercase())
                } catch (e: Exception) {
                    PlanTier.FREE
                },
                priceMonthly = (data["priceMonthly"] as? Number)?.toDouble() ?: 0.0,
                priceYearly = (data["priceYearly"] as? Number)?.toDouble() ?: 0.0,
                features = PlanFeatures.fromMap(data["features"] as? Map<String, Any> ?: emptyMap())
            )
        }
    }

    fun toFirestore(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "tier" to tier.name.lowercase(),
            "priceMonthly" to priceMonthly,
            "priceYearly" to priceYearly,
            "features" to features.toMap()
        )
    }
}

/**
 * Feature limits and capabilities for each plan
 */
data class PlanFeatures(
    val maxGroups: Int = 1,
    val maxMembersPerGroup: Int = 5,
    val maxActiveTasks: Int = 10,
    val maxStorageGB: Int = 0,
    val maxStorageMB: Int = 100,
    val maxPhotosPerMonth: Int = 5,
    val canUseBudgets: Boolean = false,
    val canUseBidding: Boolean = false,
    val canUseApprovalHierarchy: Boolean = false,
    val canUseMultimediaChat: Boolean = false,
    val canExportData: Boolean = false,
    val canUseAnalytics: Boolean = false,
    val canUseAuditLogs: Boolean = false,
    val canCustomizeNotifications: Boolean = false,
    val supportLevel: String = "standard",
    val canUseSSO: Boolean = false,
    val canUseAPI: Boolean = false,
    val canWhiteLabel: Boolean = false
) {
    companion object {
        fun fromMap(map: Map<String, Any>): PlanFeatures {
            return PlanFeatures(
                maxGroups = (map["maxGroups"] as? Number)?.toInt() ?: 1,
                maxMembersPerGroup = (map["maxMembersPerGroup"] as? Number)?.toInt() ?: 5,
                maxActiveTasks = (map["maxActiveTasks"] as? Number)?.toInt() ?: 10,
                maxStorageGB = (map["maxStorageGB"] as? Number)?.toInt() ?: 0,
                maxStorageMB = (map["maxStorageMB"] as? Number)?.toInt() ?: 100,
                maxPhotosPerMonth = (map["maxPhotosPerMonth"] as? Number)?.toInt() ?: 5,
                canUseBudgets = map["canUseBudgets"] as? Boolean ?: false,
                canUseBidding = map["canUseBidding"] as? Boolean ?: false,
                canUseApprovalHierarchy = map["canUseApprovalHierarchy"] as? Boolean ?: false,
                canUseMultimediaChat = map["canUseMultimediaChat"] as? Boolean ?: false,
                canExportData = map["canExportData"] as? Boolean ?: false,
                canUseAnalytics = map["canUseAnalytics"] as? Boolean ?: false,
                canUseAuditLogs = map["canUseAuditLogs"] as? Boolean ?: false,
                canCustomizeNotifications = map["canCustomizeNotifications"] as? Boolean ?: false,
                supportLevel = map["supportLevel"] as? String ?: "standard",
                canUseSSO = map["canUseSSO"] as? Boolean ?: false,
                canUseAPI = map["canUseAPI"] as? Boolean ?: false,
                canWhiteLabel = map["canWhiteLabel"] as? Boolean ?: false
            )
        }
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "maxGroups" to maxGroups,
            "maxMembersPerGroup" to maxMembersPerGroup,
            "maxActiveTasks" to maxActiveTasks,
            "maxStorageGB" to maxStorageGB,
            "maxStorageMB" to maxStorageMB,
            "maxPhotosPerMonth" to maxPhotosPerMonth,
            "canUseBudgets" to canUseBudgets,
            "canUseBidding" to canUseBidding,
            "canUseApprovalHierarchy" to canUseApprovalHierarchy,
            "canUseMultimediaChat" to canUseMultimediaChat,
            "canExportData" to canExportData,
            "canUseAnalytics" to canUseAnalytics,
            "canUseAuditLogs" to canUseAuditLogs,
            "canCustomizeNotifications" to canCustomizeNotifications,
            "supportLevel" to supportLevel,
            "canUseSSO" to canUseSSO,
            "canUseAPI" to canUseAPI,
            "canWhiteLabel" to canWhiteLabel
        )
    }

    fun getStorageLimitBytes(): Long {
        return if (maxStorageGB > 0) {
            maxStorageGB * 1024L * 1024L * 1024L
        } else {
            maxStorageMB * 1024L * 1024L
        }
    }
}
