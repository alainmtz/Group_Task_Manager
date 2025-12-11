package com.alainmtz.work_group_tasks.domain.services

import android.util.Log
import com.alainmtz.work_group_tasks.domain.models.Company
import com.alainmtz.work_group_tasks.domain.models.CompanyRole
import com.alainmtz.work_group_tasks.domain.models.SubscriptionStatus
import com.alainmtz.work_group_tasks.domain.models.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Service for managing companies and memberships
 */
object CompanyService {
    
    private const val TAG = "CompanyService"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Creates a personal company for a user when they upgrade from FREE plan
     * Automatically migrates all existing user data to the new company
     */
    suspend fun createPersonalCompanyOnUpgrade(
        userId: String,
        userName: String,
        userEmail: String,
        planId: String
    ): Result<Company> {
        return try {
            Log.d(TAG, "Creating personal company for user: $userId with plan: $planId")
            
            val companyId = "company_$userId"
            val now = Date()
            
            // 1. Create the company
            val company = Company(
                id = companyId,
                name = "$userName's Workspace",
                ownerId = userId,
                adminIds = listOf(userId),
                memberIds = listOf(userId),
                planId = planId,
                subscriptionStatus = SubscriptionStatus.ACTIVE,
                subscriptionStartDate = now,
                nextBillingDate = calculateNextBillingDate(now),
                activeTasksCount = 0,
                groupsCount = 0,
                storageUsedBytes = 0,
                photosUploadedThisMonth = 0,
                lastPhotoResetDate = now,
                createdAt = now
            )
            
            firestore.collection("companies")
                .document(companyId)
                .set(company.toFirestore())
                .await()
            
            Log.d(TAG, "Company created: $companyId")
            
            // 2. Update the user
            firestore.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "companyId" to companyId,
                        "role" to CompanyRole.OWNER.name
                    )
                )
                .await()
            
            Log.d(TAG, "User updated with company reference")
            
            // 3. Migrate existing groups to the company
            migrateUserGroupsToCompany(userId, companyId)
            
            // 4. Migrate existing tasks to the company
            migrateUserTasksToCompany(userId, companyId)
            
            // 5. Update company counters
            updateCompanyCounters(companyId)
            
            Log.d(TAG, "Company setup complete")
            
            Result.success(company)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating personal company", e)
            Result.failure(e)
        }
    }
    
    /**
     * Migrates all groups created by the user to the new company
     */
    private suspend fun migrateUserGroupsToCompany(userId: String, companyId: String) {
        try {
            val groupsSnapshot = firestore.collection("groups")
                .whereEqualTo("creatorId", userId)
                .get()
                .await()
            
            Log.d(TAG, "Migrating ${groupsSnapshot.size()} groups to company $companyId")
            
            groupsSnapshot.documents.forEach { doc ->
                doc.reference.update("companyId", companyId).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating groups", e)
            // Don't fail the whole process
        }
    }
    
    /**
     * Migrates all tasks created by the user to the new company
     */
    private suspend fun migrateUserTasksToCompany(userId: String, companyId: String) {
        try {
            val tasksSnapshot = firestore.collection("tasks")
                .whereEqualTo("creatorId", userId)
                .get()
                .await()
            
            Log.d(TAG, "Migrating ${tasksSnapshot.size()} tasks to company $companyId")
            
            tasksSnapshot.documents.forEach { doc ->
                doc.reference.update("companyId", companyId).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating tasks", e)
        }
    }
    
    /**
     * Updates company counters for groups and tasks
     */
    private suspend fun updateCompanyCounters(companyId: String) {
        try {
            // Count groups
            val groupsCount = firestore.collection("groups")
                .whereEqualTo("companyId", companyId)
                .get()
                .await()
                .size()
            
            // Count active tasks
            val tasksSnapshot = firestore.collection("tasks")
                .whereEqualTo("companyId", companyId)
                .get()
                .await()
            
            val activeTasksCount = tasksSnapshot.documents.count { doc ->
                val status = doc.getString("status") ?: ""
                status.equals("pending", ignoreCase = true) || 
                status.equals("in_progress", ignoreCase = true)
            }
            
            // Update company
            firestore.collection("companies")
                .document(companyId)
                .update(
                    mapOf(
                        "groupsCount" to groupsCount,
                        "activeTasksCount" to activeTasksCount
                    )
                )
                .await()
            
            Log.d(TAG, "Company counters updated: groups=$groupsCount, tasks=$activeTasksCount")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating company counters", e)
        }
    }
    
    /**
     * Updates the company name
     */
    suspend fun updateCompanyName(companyId: String, newName: String): Result<Unit> {
        return try {
            firestore.collection("companies")
                .document(companyId)
                .update("name", newName)
                .await()
            
            Log.d(TAG, "Company name updated: $companyId -> $newName")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating company name", e)
            Result.failure(e)
        }
    }
    
    /**
     * Adds a member to the company by email
     */
    suspend fun addMemberToCompany(
        companyId: String,
        userEmail: String,
        role: CompanyRole = CompanyRole.MEMBER
    ): Result<Unit> {
        return try {
            // 1. Find user by email
            val userSnapshot = firestore.collection("users")
                .whereEqualTo("email", userEmail)
                .limit(1)
                .get()
                .await()
            
            if (userSnapshot.isEmpty) {
                return Result.failure(Exception("User not found with email: $userEmail"))
            }
            
            val userDoc = userSnapshot.documents[0]
            val userId = userDoc.id
            val existingCompanyId = userDoc.getString("companyId")
            
            // 2. Check if user is already in another company
            if (existingCompanyId != null && existingCompanyId != companyId) {
                return Result.failure(Exception("User is already in another company"))
            }
            
            // 3. Add to company
            val updates = mutableMapOf<String, Any>(
                "memberIds" to FieldValue.arrayUnion(userId)
            )
            
            if (role == CompanyRole.ADMIN) {
                updates["adminIds"] = FieldValue.arrayUnion(userId)
            }
            
            firestore.collection("companies")
                .document(companyId)
                .update(updates)
                .await()
            
            // 4. Update user
            firestore.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "companyId" to companyId,
                        "role" to role.name
                    )
                )
                .await()
            
            Log.d(TAG, "Member added: $userId to company $companyId with role $role")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding member", e)
            Result.failure(e)
        }
    }
    
    /**
     * Removes a member from the company
     */
    suspend fun removeMemberFromCompany(companyId: String, userId: String): Result<Unit> {
        return try {
            // 1. Check if user is the owner
            val companyDoc = firestore.collection("companies")
                .document(companyId)
                .get()
                .await()
            
            val ownerId = companyDoc.getString("ownerId")
            if (ownerId == userId) {
                return Result.failure(Exception("Cannot remove the company owner"))
            }
            
            // 2. Remove from company
            firestore.collection("companies")
                .document(companyId)
                .update(
                    mapOf(
                        "memberIds" to FieldValue.arrayRemove(userId),
                        "adminIds" to FieldValue.arrayRemove(userId)
                    )
                )
                .await()
            
            // 3. Update user (reverts to FREE plan)
            firestore.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "companyId" to FieldValue.delete(),
                        "role" to FieldValue.delete()
                    )
                )
                .await()
            
            Log.d(TAG, "Member removed: $userId from company $companyId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing member", e)
            Result.failure(e)
        }
    }
    
    /**
     * Updates a member's role
     */
    suspend fun updateMemberRole(
        companyId: String,
        userId: String,
        newRole: CompanyRole
    ): Result<Unit> {
        return try {
            // Update company
            val updates = if (newRole == CompanyRole.ADMIN) {
                mapOf("adminIds" to FieldValue.arrayUnion(userId))
            } else {
                mapOf("adminIds" to FieldValue.arrayRemove(userId))
            }
            
            firestore.collection("companies")
                .document(companyId)
                .update(updates)
                .await()
            
            // Update user
            firestore.collection("users")
                .document(userId)
                .update("role", newRole.name)
                .await()
            
            Log.d(TAG, "Member role updated: $userId to $newRole in company $companyId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating member role", e)
            Result.failure(e)
        }
    }
    
    /**
     * Gets all members of a company with their user data
     */
    suspend fun getCompanyMembers(companyId: String): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("companyId", companyId)
                .get()
                .await()
            
            val members = snapshot.documents.mapNotNull { 
                User.fromFirestore(it)
            }
            
            Log.d(TAG, "Loaded ${members.size} members for company $companyId")
            Result.success(members)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting company members", e)
            Result.failure(e)
        }
    }
    
    /**
     * Calculates next billing date (30 days from now)
     */
    private fun calculateNextBillingDate(startDate: Date): Date {
        val calendar = java.util.Calendar.getInstance()
        calendar.time = startDate
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 30)
        return calendar.time
    }
}
