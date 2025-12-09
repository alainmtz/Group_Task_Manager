# STAGE 2: Subscription Data Model - IMPLEMENTATION COMPLETE ✅

**Date**: December 9, 2025
**Status**: ✅ Complete - All data models implemented and compiled successfully

## Overview

STAGE 2 focused on creating the subscription data infrastructure without UI changes or billing integration. All new models support the 4-tier pricing system (FREE, PRO, BUSINESS, ENTERPRISE) with feature flags and usage tracking.

---

## New Models Created

### 1. `Plan.kt` ✅
**Location**: `app/src/main/kotlin/com/alainmtz/work_group_tasks/domain/models/Plan.kt`

**Components**:
- `PlanTier` enum: FREE, PRO, BUSINESS, ENTERPRISE
- `Plan` data class: Subscription plan entity
- `PlanFeatures` data class: 18 feature flags and limits
- Firestore serialization/deserialization
- `getStorageLimitBytes()` helper method

**Key Features**:
```kotlin
data class PlanFeatures(
    maxGroups: Int,              // -1 = unlimited
    maxMembersPerGroup: Int,
    maxActiveTasks: Int,
    maxStorageGB: Int,
    maxStorageMB: Int,
    maxPhotosPerMonth: Int,
    canUseBudgets: Boolean,
    canUseBidding: Boolean,
    canUseApprovalHierarchy: Boolean,
    canUseMultimediaChat: Boolean,
    canExportData: Boolean,
    canUseAnalytics: Boolean,
    canUseAuditLogs: Boolean,
    canCustomizeNotifications: Boolean,
    supportLevel: String,
    canUseSSO: Boolean,
    canUseAPI: Boolean,
    canWhiteLabel: Boolean
)
```

---

### 2. `Company.kt` ✅
**Location**: `app/src/main/kotlin/com/alainmtz/work_group_tasks/domain/models/Company.kt`

**Purpose**: Represents an organization/company with subscription billing

**Fields**:
- Ownership: `ownerId`, `adminIds`, `memberIds`
- Billing: `planId`, `planTier`, `subscriptionStatus`, `googlePlayPurchaseToken`
- Dates: `subscriptionStartDate`, `nextBillingDate`, `lastPhotoResetDate`
- Usage Tracking: `activeTasksCount`, `groupsCount`, `storageUsedBytes`, `photosUploadedThisMonth`

**Helper Methods**:
- `isOwner(userId)`: Check if user is company owner
- `isAdmin(userId)`: Check if user is admin
- `isMember(userId)`: Check if user is member

**SubscriptionStatus Enum**:
- ACTIVE: Subscription is active
- CANCELED: User canceled, access until period end
- EXPIRED: Subscription expired
- GRACE_PERIOD: Payment failed, grace period active
- ON_HOLD: Suspended by admin/billing issue

---

### 3. `CompanyRole.kt` ✅
**Location**: `app/src/main/kotlin/com/alainmtz/work_group_tasks/domain/models/CompanyRole.kt`

**Purpose**: Define member roles and permissions

**Roles**:
```kotlin
enum class CompanyRole {
    OWNER,    // Full control, can manage billing
    ADMIN,    // Can manage members and settings
    MEMBER    // Regular user access
}
```

**Permission Methods**:
- `canManageBilling()`: Only OWNER
- `canManageMembers()`: OWNER or ADMIN
- `canManageSettings()`: OWNER or ADMIN

---

### 4. `PlanDefaults.kt` ✅
**Location**: `app/src/main/kotlin/com/alainmtz/work_group_tasks/domain/models/PlanDefaults.kt`

**Purpose**: Predefined configurations for all 4 subscription tiers

**Plans Defined**:

#### FREE Plan
- 1 group, 5 members, 10 tasks
- 100MB storage, 5 photos/month
- Text-only chat
- ❌ No budgets/bidding
- ❌ No advanced features

#### PRO Plan ($6.99/mo, $69/year)
- Unlimited groups, 15 members, 50 tasks
- 5GB storage, unlimited photos
- ✅ Budgets & bidding
- ✅ Multimedia chat
- ✅ Basic analytics
- Priority support

#### BUSINESS Plan ($39/mo, $399/year)
- Unlimited groups, 50 members, 200 tasks
- 50GB storage, unlimited photos
- ✅ All PRO features
- ✅ Approval hierarchy
- ✅ Data export
- ✅ Audit logs
- ✅ Custom notifications
- Dedicated support

#### ENTERPRISE Plan (Custom Pricing)
- Unlimited everything
- ✅ All BUSINESS features
- ✅ SSO/SAML
- ✅ API access
- ✅ White-label
- ✅ Google Workspace integration
- Dedicated account manager

**Helper Methods**:
- `getPlanById(id)`: Get plan by ID string
- `getPlanByTier(tier)`: Get plan by enum
- `getAllPlans()`: Get all 4 plans

---

### 5. `FeatureFlags.kt` ✅
**Location**: `app/src/main/kotlin/com/alainmtz/work_group_tasks/domain/services/FeatureFlags.kt`

**Purpose**: Runtime feature checking and limit validation

**Feature Check Methods** (all return `Pair<Boolean, String?>`):
- `canCreateGroup(company, plan)`: Check group limit
- `canCreateTask(company, plan)`: Check task limit
- `canAddMember(count, plan)`: Check member limit
- `canUploadPhoto(company, plan)`: Check monthly photo quota
- `canUploadFile(company, plan, fileSize)`: Check storage quota
- `canUseBudgets(plan)`: Check budgeting feature
- `canUseApprovalHierarchy(plan)`: Check approval feature
- `canSendMultimedia(plan)`: Check multimedia chat
- `canUseAnalytics(plan)`: Check analytics access
- `canExportData(plan)`: Check export feature

**Usage Tracking**:
- `getStorageUsagePercentage(company, plan)`: 0-100%
- `getPhotoUsagePercentage(company, plan)`: 0-100%

**Upgrade Suggestions**:
- Returns user-friendly messages: "Upgrade to PRO for unlimited uploads"
- Automatically suggests next tier

---

## Existing Models Updated

### 6. `User.kt` ✅
**Changes**:
```kotlin
data class User(
    // ... existing fields ...
    val companyId: String? = null,           // ✅ NEW
    val role: String = "member",             // ✅ NEW
    val storageUsedBytes: Long = 0L,        // ✅ NEW
    val uploadCountThisMonth: Int = 0       // ✅ NEW
)
```

---

### 7. `Group.kt` ✅
**Changes**:
```kotlin
data class Group(
    // ... existing fields ...
    val companyId: String? = null  // ✅ NEW
)
```

---

### 8. `Task.kt` ✅
**Changes**:
```kotlin
data class Task(
    // ... existing fields ...
    val companyId: String? = null,                  // ✅ NEW
    val requiresApproval: Boolean = false,          // ✅ NEW
    val approvalHierarchy: List<String> = emptyList(), // ✅ NEW
    val approvalStatus: String = "none"            // ✅ NEW
)
```

---

### 9. `Earning.kt` ✅
**Changes**:
```kotlin
data class Earning(
    // ... existing fields ...
    val companyId: String? = null  // ✅ NEW
)
```

---

## Compilation Status

✅ **BUILD SUCCESSFUL in 1m 14s**
- 41 actionable tasks: 4 executed, 37 up-to-date
- All new models compile without errors
- Only unchecked cast warnings (expected for Firestore deserialization)
- Ready for STAGE 3 integration

---

## Next Steps: STAGE 3 - Google Play Billing Integration

**Not Started** - Requires:

1. **Google Play Console Setup**:
   - Create subscription products (PRO monthly/yearly, BUSINESS monthly/yearly)
   - Configure pricing and billing cycles
   - Set up proration rules

2. **BillingClient Implementation**:
   - Add `com.android.billingclient:billing-ktx` dependency
   - Initialize BillingClient
   - Query available products
   - Launch purchase flow
   - Handle purchase updates
   - Restore purchases

3. **Backend Verification** (Firebase Functions):
   - Create `verifyPurchase` Cloud Function
   - Create `updateSubscription` Cloud Function
   - Create `handleWebhook` for real-time notifications
   - Implement receipt validation with Google Play Developer API

4. **Company Service**:
   - Create `CompanyService.kt` to manage company creation
   - Load company data for authenticated user
   - Update usage counters (tasks, storage, photos)
   - Reset monthly photo counters

5. **Subscription Service**:
   - Create `SubscriptionService.kt` for billing operations
   - Handle subscription lifecycle (new, renewal, upgrade, downgrade, cancel)
   - Sync subscription status with Firestore

---

## Migration Strategy (For Later)

**Goal**: Migrate existing users to company-centric model

**Steps** (Not implemented yet):
1. Create Cloud Function `migrateUsersToCompanies`
2. For each existing user:
   - Create personal company: `{userName}'s Workspace`
   - Set user as owner
   - Link existing groups/tasks to company
   - Set plan to FREE
3. Update User documents with `companyId`
4. Update all Groups with `companyId`
5. Update all Tasks with `companyId`
6. Zero data loss, zero downtime

---

## Summary

✅ **STAGE 2 Complete**: All subscription data models implemented
- 5 new files created (Plan, Company, CompanyRole, PlanDefaults, FeatureFlags)
- 4 existing models updated (User, Group, Task, Earning)
- Feature flag system ready for runtime checks
- Usage tracking fields in place
- Backward compatible (all new fields nullable or with defaults)
- Compiled successfully with no errors

**Total Files**: 9 files created/modified
**Total Lines**: ~800 lines of new code
**Ready for**: STAGE 3 Google Play Billing integration
