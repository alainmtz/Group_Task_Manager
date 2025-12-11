# Company & Plan Integration Guide

## Overview

The app now automatically loads and tracks the current user's company and subscription plan at runtime. Feature flags are enforced based on the user's plan tier.

## Architecture

### CompanyPlanProvider (Singleton)

- **Location**: `domain/services/CompanyPlanProvider.kt`
- **Purpose**: Centralized provider for company and plan data with real-time updates
- **Initialization**: Auto-initialized in `CollaborativeTasksApplication.onCreate()`
- **Features**:
  - Listens to auth state changes
  - Loads company and plan on sign-in
  - Real-time Firestore listeners for updates
  - Clears data on sign-out

### StateFlows Available

```kotlin
CompanyPlanProvider.currentCompany: StateFlow<Company?>
CompanyPlanProvider.currentPlan: StateFlow<Plan>
CompanyPlanProvider.isLoading: StateFlow<Boolean>
```

### FeatureFlags (Runtime Checks)

- **Location**: `domain/services/FeatureFlags.kt`
- **Purpose**: Runtime feature checking based on plan limits
- **Methods**: Return `Pair<Boolean, String?>` where:
  - `Boolean`: Can perform action
  - `String?`: Error message if action is blocked (null if allowed)

## Usage in ViewModels

### Basic Pattern

```kotlin
class MyViewModel : ViewModel() {
    val currentCompany = CompanyPlanProvider.currentCompany
    val currentPlan = CompanyPlanProvider.currentPlan

    fun checkFeature() {
        viewModelScope.launch {
            combine(currentCompany, currentPlan) { company, plan ->
                // Check features when company or plan changes
                val (canCreate, message) = FeatureFlags.canCreateGroup(company!!, plan)
                if (!canCreate) {
                    _error.value = message
                }
            }.collect()
        }
    }
}
```

### Example: PlanAwareViewModel

See `ui/viewmodels/PlanAwareViewModel.kt` for a complete example showing:

- How to observe company and plan
- How to call FeatureFlags methods
- How to expose feature states to UI
- How to force reload after plan changes

## Usage in Composables

### Collect State

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel = viewModel()) {
    val company by CompanyPlanProvider.currentCompany.collectAsState()
    val plan by CompanyPlanProvider.currentPlan.collectAsState()

    // Check before action
    Button(
        onClick = {
            val (canCreate, message) = FeatureFlags.canCreateGroup(company!!, plan)
            if (canCreate) {
                // Proceed with creation
            } else {
                // Show upgrade prompt with message
            }
        }
    ) {
        Text("Create Group")
    }
}
```

## Available Feature Checks

### Quantity Limits

- `canCreateGroup(company, plan)` - Check group creation limit
- `canCreateTask(company, plan)` - Check active task limit
- `canAddMember(currentCount, plan)` - Check members per group limit
- `canUploadPhoto(company, plan)` - Check monthly photo upload limit
- `canUploadFile(company, plan, fileSize)` - Check storage quota

### Boolean Features

- `canUseBudgets(plan)` - PRO+ required
- `canUseBidding(plan)` - PRO+ required (if implemented)
- `canUseApprovalHierarchy(plan)` - BUSINESS+ required
- `canUseMultimediaChat(plan)` - PRO+ required
- `canUseAnalytics(plan)` - PRO+ required
- `canExportData(plan)` - BUSINESS+ required
- `canUseAuditLogs(plan)` - BUSINESS+ required
- `canUseSSO(plan)` - ENTERPRISE required
- `canUseAPI(plan)` - ENTERPRISE required
- `canWhiteLabel(plan)` - ENTERPRISE required

### Utility Methods

- `getSupportLevel(plan)` - Returns support tier string
- `getStorageUsagePercentage(company, plan)` - Returns 0-100
- `getPhotoUsagePercentage(company, plan)` - Returns 0-100

## Plan Tiers & Features

| Feature               | FREE  | PRO       | BUSINESS  | ENTERPRISE|
|---------              |------ |-----      |---------- |-----------|
| Groups                | 1     | Unlimited | Unlimited | Unlimited |
| Members/Group         | 5     | 15        | 50        | Unlimited |
| Active Tasks          | 10    | 50        | 200       | Unlimited |
| Storage               | 100MB | 5GB       | 50GB      | Unlimited |
| Photos/Month          | 5     | 50        | 200       | Unlimited |
| Budgets               | ❌    | ✅        | ✅        |   ✅      |
| Approval Hierarchy    | ❌    | ❌        | ✅        |   ✅      |
| Analytics             | ❌    | ✅        | ✅        |   ✅      |
| Data Export           | ❌    | ❌        | ✅        |   ✅      |
| Audit Logs            | ❌    | ❌        | ✅        |   ✅      |
| SSO                   |   ❌  | ❌        | ❌        |   ✅      |
| API Access            | ❌    | ❌        | ❌        |   ✅      |
| White Label           | ❌    | ❌        | ❌        |   ✅      |

## Force Reload

After plan changes (e.g., upgrade, downgrade):

```kotlin
viewModelScope.launch {
    CompanyPlanProvider.reload()
}
```

## Firestore Structure

### User Document

```text
/users/{userId}
  companyId: string  // Reference to company document
  ...
```

### Company Document

``` kotlin
/companies/{companyId}
  name: string
  ownerId: string
  planId: string           // e.g., "free", "pro", "business", "enterprise"
  planTier: string         // "free", "pro", "business", "enterprise"
  groupsCount: number
  activeTasksCount: number
  storageUsedBytes: number
  photosUploadedThisMonth: number
  ...
```

### Plan Document

``` kotlin
/plans/{planId}
  name: string
  tier: string
  priceMonthly: number
  priceYearly: number
  features: {
    maxGroups: number       // -1 = unlimited
    maxMembersPerGroup: number
    maxActiveTasks: number
    maxStorageGB: number
    canUseBudgets: boolean
    canUseSSO: boolean
    ...
  }
```

## Next Steps

1. Add unit tests for FeatureFlags
2. Implement upgrade prompts UI
3. Add usage indicators (e.g., progress bars for storage)
4. Implement in-app purchase flow for plan upgrades
