package com.alainmtz.work_group_tasks.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alainmtz.work_group_tasks.domain.models.Company
import com.alainmtz.work_group_tasks.domain.models.Plan
import com.alainmtz.work_group_tasks.domain.services.CompanyPlanProvider
import com.alainmtz.work_group_tasks.domain.services.FeatureFlags
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Example ViewModel showing how to use CompanyPlanProvider and FeatureFlags
 * for runtime feature checking and limit enforcement.
 */
open class PlanAwareViewModel : ViewModel() {

    // Expose company and plan from the provider
    val currentCompany: StateFlow<Company?> = CompanyPlanProvider.currentCompany
    val currentPlan: StateFlow<Plan> = CompanyPlanProvider.currentPlan
    val isLoadingPlan: StateFlow<Boolean> = CompanyPlanProvider.isLoading

    // Derived states for UI
    private val _canCreateGroup = MutableStateFlow<Pair<Boolean, String?>>(Pair(true, null))
    val canCreateGroup: StateFlow<Pair<Boolean, String?>> = _canCreateGroup.asStateFlow()

    private val _canCreateTask = MutableStateFlow<Pair<Boolean, String?>>(Pair(true, null))
    val canCreateTask: StateFlow<Pair<Boolean, String?>> = _canCreateTask.asStateFlow()

    private val _canUseBudgets = MutableStateFlow<Pair<Boolean, String?>>(Pair(false, null))
    val canUseBudgets: StateFlow<Pair<Boolean, String?>> = _canUseBudgets.asStateFlow()

    private val _canUseAnalytics = MutableStateFlow<Pair<Boolean, String?>>(Pair(false, null))
    val canUseAnalytics: StateFlow<Pair<Boolean, String?>> = _canUseAnalytics.asStateFlow()

    private val _canUseSSO = MutableStateFlow<Pair<Boolean, String?>>(Pair(false, null))
    val canUseSSO: StateFlow<Pair<Boolean, String?>> = _canUseSSO.asStateFlow()

    private val _storageUsagePercent = MutableStateFlow(0)
    val storageUsagePercent: StateFlow<Int> = _storageUsagePercent.asStateFlow()

    // Upgrade prompt for when limits are reached
    protected val _upgradePrompt = MutableStateFlow<String?>(null)
    val upgradePrompt: StateFlow<String?> = _upgradePrompt.asStateFlow()

    init {
        // Observe company and plan changes, then update feature flags
        viewModelScope.launch {
            combine(currentCompany, currentPlan) { company, plan ->
                Pair(company, plan)
            }.collect { (company, plan) ->
                updateFeatureFlags(company, plan)
            }
        }
    }

    /**
     * Update all feature flag checks when company or plan changes
     */
    private fun updateFeatureFlags(company: Company?, plan: Plan) {
        if (company == null) {
            // No company - use default FREE limits
            _canCreateGroup.value = Pair(false, "Please join or create a company first")
            _canCreateTask.value = Pair(false, "Please join or create a company first")
            _canUseBudgets.value = FeatureFlags.canUseBudgets(plan)
            _canUseAnalytics.value = FeatureFlags.canUseAnalytics(plan)
            _canUseSSO.value = FeatureFlags.canUseSSO(plan)
            _storageUsagePercent.value = 0
            return
        }

        // Check limits based on current company state
        _canCreateGroup.value = FeatureFlags.canCreateGroup(company, plan)
        _canCreateTask.value = FeatureFlags.canCreateTask(company, plan)
        _canUseBudgets.value = FeatureFlags.canUseBudgets(plan)
        _canUseAnalytics.value = FeatureFlags.canUseAnalytics(plan)
        _canUseSSO.value = FeatureFlags.canUseSSO(plan)
        _storageUsagePercent.value = FeatureFlags.getStorageUsagePercentage(company, plan)
    }

    /**
     * Example: Check if user can add member to a group
     */
    fun canAddMemberToGroup(currentMemberCount: Int): Pair<Boolean, String?> {
        val plan = currentPlan.value
        return FeatureFlags.canAddMember(currentMemberCount, plan)
    }

    /**
     * Example: Check if user can upload a file
     */
    fun canUploadFile(fileSizeBytes: Long): Pair<Boolean, String?> {
        val company = currentCompany.value ?: return Pair(false, "No company found")
        val plan = currentPlan.value
        return FeatureFlags.canUploadFile(company, plan, fileSizeBytes)
    }

    /**
     * Check if user can upload a photo (monthly limit)
     */
    fun canUploadPhoto(): Pair<Boolean, String?> {
        val company = currentCompany.value ?: return Pair(false, "No company found")
        val plan = currentPlan.value
        return FeatureFlags.canUploadPhoto(company, plan)
    }

    /**
     * Example: Check if user can use approval hierarchy
     */
    fun canUseApprovalHierarchy(): Pair<Boolean, String?> {
        val plan = currentPlan.value
        return FeatureFlags.canUseApprovalHierarchy(plan)
    }

    /**
     * Example: Get support level for display
     */
    fun getSupportLevel(): String {
        val plan = currentPlan.value
        return FeatureFlags.getSupportLevel(plan)
    }

    /**
     * Clear upgrade prompt after user sees it
     */
    fun clearUpgradePrompt() {
        _upgradePrompt.value = null
    }

    /**
     * Force reload plan (e.g., after purchase)
     */
    fun reloadPlan() {
        viewModelScope.launch {
            CompanyPlanProvider.reload()
        }
    }
}
