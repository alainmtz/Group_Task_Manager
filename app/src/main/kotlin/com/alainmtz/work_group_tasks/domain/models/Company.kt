package com.alainmtz.work_group_tasks.domain.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Company data model - Represents an organization with subscription
 */
data class Company(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val adminIds: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    val planId: String = "free",
    val planTier: PlanTier = PlanTier.FREE,
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    val googlePlayPurchaseToken: String? = null,
    val subscriptionStartDate: Date? = null,
    val nextBillingDate: Date? = null,
    val activeTasksCount: Int = 0,
    val groupsCount: Int = 0,
    val storageUsedBytes: Long = 0,
    val photosUploadedThisMonth: Int = 0,
    val lastPhotoResetDate: Date? = null,
    val createdAt: Date = Date()
) {
    companion object {
        fun fromFirestore(doc: DocumentSnapshot): Company {
            val data = doc.data ?: return Company()
            return Company(
                id = doc.id,
                name = data["name"] as? String ?: "",
                ownerId = data["ownerId"] as? String ?: "",
                adminIds = (data["adminIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                memberIds = (data["memberIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                planId = data["planId"] as? String ?: "free",
                planTier = try {
                    PlanTier.valueOf((data["planTier"] as? String ?: "FREE").uppercase())
                } catch (e: Exception) {
                    PlanTier.FREE
                },
                subscriptionStatus = try {
                    SubscriptionStatus.valueOf((data["subscriptionStatus"] as? String ?: "ACTIVE").uppercase())
                } catch (e: Exception) {
                    SubscriptionStatus.ACTIVE
                },
                googlePlayPurchaseToken = data["googlePlayPurchaseToken"] as? String,
                subscriptionStartDate = (data["subscriptionStartDate"] as? Timestamp)?.toDate(),
                nextBillingDate = (data["nextBillingDate"] as? Timestamp)?.toDate(),
                activeTasksCount = (data["activeTasksCount"] as? Number)?.toInt() ?: 0,
                groupsCount = (data["groupsCount"] as? Number)?.toInt() ?: 0,
                storageUsedBytes = (data["storageUsedBytes"] as? Number)?.toLong() ?: 0L,
                photosUploadedThisMonth = (data["photosUploadedThisMonth"] as? Number)?.toInt() ?: 0,
                lastPhotoResetDate = (data["lastPhotoResetDate"] as? Timestamp)?.toDate(),
                createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date()
            )
        }
    }

    fun toFirestore(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "ownerId" to ownerId,
            "adminIds" to adminIds,
            "memberIds" to memberIds,
            "planId" to planId,
            "planTier" to planTier.name.lowercase(),
            "subscriptionStatus" to subscriptionStatus.name.lowercase(),
            "googlePlayPurchaseToken" to googlePlayPurchaseToken,
            "subscriptionStartDate" to subscriptionStartDate?.let { Timestamp(it) },
            "nextBillingDate" to nextBillingDate?.let { Timestamp(it) },
            "activeTasksCount" to activeTasksCount,
            "groupsCount" to groupsCount,
            "storageUsedBytes" to storageUsedBytes,
            "photosUploadedThisMonth" to photosUploadedThisMonth,
            "lastPhotoResetDate" to lastPhotoResetDate?.let { Timestamp(it) },
            "createdAt" to Timestamp(createdAt)
        )
    }

    fun isOwner(userId: String): Boolean = userId == ownerId
    fun isAdmin(userId: String): Boolean = userId in adminIds || isOwner(userId)
    fun isMember(userId: String): Boolean = userId in memberIds || isAdmin(userId)
}

/**
 * Subscription status enum
 */
enum class SubscriptionStatus {
    ACTIVE,
    CANCELED,
    EXPIRED,
    GRACE_PERIOD,
    ON_HOLD
}
