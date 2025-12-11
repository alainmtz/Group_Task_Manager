package com.alainmtz.work_group_tasks.domain.services

import com.alainmtz.work_group_tasks.domain.models.Company
import com.alainmtz.work_group_tasks.domain.models.Plan
import com.alainmtz.work_group_tasks.domain.models.PlanDefaults
import com.alainmtz.work_group_tasks.domain.models.PlanTier
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

/**
 * Unit tests for FeatureFlags service
 */
class FeatureFlagsTest {

    // Test helper to create a test company
    private fun createTestCompany(
        groupsCount: Int = 0,
        activeTasksCount: Int = 0,
        storageUsedBytes: Long = 0L,
        photosUploadedThisMonth: Int = 0
    ): Company {
        return Company(
            id = "test_company",
            name = "Test Company",
            ownerId = "test_owner",
            groupsCount = groupsCount,
            activeTasksCount = activeTasksCount,
            storageUsedBytes = storageUsedBytes,
            photosUploadedThisMonth = photosUploadedThisMonth,
            createdAt = Date()
        )
    }

    // ========================================
    // GROUP CREATION TESTS
    // ========================================

    @Test
    fun `canCreateGroup returns true when under FREE plan limit`() {
        val company = createTestCompany(groupsCount = 0)
        val plan = PlanDefaults.FREE
        
        val (canCreate, message) = FeatureFlags.canCreateGroup(company, plan)
        
        assertTrue("Should allow creating first group on FREE plan", canCreate)
        assertNull("Should not have error message", message)
    }

    @Test
    fun `canCreateGroup returns false when FREE plan limit reached`() {
        val company = createTestCompany(groupsCount = 1) // FREE allows max 1 group
        val plan = PlanDefaults.FREE
        
        val (canCreate, message) = FeatureFlags.canCreateGroup(company, plan)
        
        assertFalse("Should block group creation at FREE limit", canCreate)
        assertNotNull("Should provide upgrade message", message)
        assertTrue("Message should mention limit", message!!.contains("1"))
        assertTrue("Message should suggest upgrade", message.contains("PRO"))
    }

    @Test
    fun `canCreateGroup returns true for PRO plan with unlimited groups`() {
        val company = createTestCompany(groupsCount = 50)
        val plan = PlanDefaults.PRO
        
        val (canCreate, message) = FeatureFlags.canCreateGroup(company, plan)
        
        assertTrue("PRO plan should allow unlimited groups", canCreate)
        assertNull("Should not have error message", message)
    }

    @Test
    fun `canCreateGroup returns true for ENTERPRISE plan with unlimited groups`() {
        val company = createTestCompany(groupsCount = 1000)
        val plan = PlanDefaults.ENTERPRISE
        
        val (canCreate, message) = FeatureFlags.canCreateGroup(company, plan)
        
        assertTrue("ENTERPRISE plan should allow unlimited groups", canCreate)
        assertNull("Should not have error message", message)
    }

    // ========================================
    // TASK CREATION TESTS
    // ========================================

    @Test
    fun `canCreateTask returns true when under FREE plan limit`() {
        val company = createTestCompany(activeTasksCount = 5)
        val plan = PlanDefaults.FREE
        
        val (canCreate, message) = FeatureFlags.canCreateTask(company, plan)
        
        assertTrue("Should allow task creation under FREE limit (10)", canCreate)
        assertNull("Should not have error message", message)
    }

    @Test
    fun `canCreateTask returns false when FREE plan limit reached`() {
        val company = createTestCompany(activeTasksCount = 10) // FREE allows max 10 tasks
        val plan = PlanDefaults.FREE
        
        val (canCreate, message) = FeatureFlags.canCreateTask(company, plan)
        
        assertFalse("Should block task creation at FREE limit", canCreate)
        assertNotNull("Should provide upgrade message", message)
        assertTrue("Message should mention limit", message!!.contains("10"))
    }

    @Test
    fun `canCreateTask returns true for PRO plan under limit`() {
        val company = createTestCompany(activeTasksCount = 25)
        val plan = PlanDefaults.PRO // Max 50 tasks
        
        val (canCreate, message) = FeatureFlags.canCreateTask(company, plan)
        
        assertTrue("Should allow task creation under PRO limit", canCreate)
        assertNull("Should not have error message", message)
    }

    @Test
    fun `canCreateTask returns false when PRO plan limit reached`() {
        val company = createTestCompany(activeTasksCount = 50)
        val plan = PlanDefaults.PRO
        
        val (canCreate, message) = FeatureFlags.canCreateTask(company, plan)
        
        assertFalse("Should block task creation at PRO limit", canCreate)
        assertNotNull("Should provide upgrade message", message)
        assertTrue("Message should suggest BUSINESS upgrade", message!!.contains("BUSINESS"))
    }

    @Test
    fun `canCreateTask returns true for ENTERPRISE plan with unlimited tasks`() {
        val company = createTestCompany(activeTasksCount = 5000)
        val plan = PlanDefaults.ENTERPRISE
        
        val (canCreate, message) = FeatureFlags.canCreateTask(company, plan)
        
        assertTrue("ENTERPRISE should allow unlimited tasks", canCreate)
        assertNull("Should not have error message", message)
    }

    // ========================================
    // MEMBER ADDITION TESTS
    // ========================================

    @Test
    fun `canAddMember returns true when under FREE plan limit`() {
        val plan = PlanDefaults.FREE // Max 5 members
        
        val (canAdd, message) = FeatureFlags.canAddMember(currentMemberCount = 3, plan)
        
        assertTrue("Should allow adding member under FREE limit", canAdd)
        assertNull("Should not have error message", message)
    }

    @Test
    fun `canAddMember returns false when FREE plan limit reached`() {
        val plan = PlanDefaults.FREE // Max 5 members
        
        val (canAdd, message) = FeatureFlags.canAddMember(currentMemberCount = 5, plan)
        
        assertFalse("Should block member addition at FREE limit", canAdd)
        assertNotNull("Should provide upgrade message", message)
        assertTrue("Message should mention limit", message!!.contains("5"))
    }

    @Test
    fun `canAddMember returns true for PRO plan under limit`() {
        val plan = PlanDefaults.PRO // Max 15 members
        
        val (canAdd, message) = FeatureFlags.canAddMember(currentMemberCount = 10, plan)
        
        assertTrue("Should allow adding member under PRO limit", canAdd)
        assertNull("Should not have error message", message)
    }

    @Test
    fun `canAddMember returns true for ENTERPRISE with unlimited members`() {
        val plan = PlanDefaults.ENTERPRISE
        
        val (canAdd, message) = FeatureFlags.canAddMember(currentMemberCount = 10000, plan)
        
        assertTrue("ENTERPRISE should allow unlimited members", canAdd)
        assertNull("Should not have error message", message)
    }

    // ========================================
    // PHOTO UPLOAD TESTS
    // ========================================

    @Test
    fun `canUploadPhoto returns true when under FREE plan limit`() {
        val company = createTestCompany(photosUploadedThisMonth = 3)
        val plan = PlanDefaults.FREE // Max 5 photos/month
        
        val (canUpload, message) = FeatureFlags.canUploadPhoto(company, plan)
        
        assertTrue("Should allow photo upload under FREE limit", canUpload)
        assertNull("Should not have error message", message)
    }

    @Test
    fun `canUploadPhoto returns false when FREE plan limit reached`() {
        val company = createTestCompany(photosUploadedThisMonth = 5)
        val plan = PlanDefaults.FREE
        
        val (canUpload, message) = FeatureFlags.canUploadPhoto(company, plan)
        
        assertFalse("Should block photo upload at FREE limit", canUpload)
        assertNotNull("Should provide upgrade message", message)
        assertTrue("Message should mention limit", message!!.contains("5"))
    }

    @Test
    fun `canUploadPhoto returns true for ENTERPRISE with unlimited photos`() {
        val company = createTestCompany(photosUploadedThisMonth = 1000)
        val plan = PlanDefaults.ENTERPRISE
        
        val (canUpload, message) = FeatureFlags.canUploadPhoto(company, plan)
        
        assertTrue("ENTERPRISE should allow unlimited photos", canUpload)
        assertNull("Should not have error message", message)
    }

    // ========================================
    // STORAGE TESTS
    // ========================================

    @Test
    fun `canUploadFile returns true when under FREE plan storage limit`() {
        val company = createTestCompany(storageUsedBytes = 50 * 1024 * 1024) // 50MB used
        val plan = PlanDefaults.FREE // 100MB limit
        val fileSizeBytes = 10 * 1024 * 1024L // 10MB file
        
        val (canUpload, message) = FeatureFlags.canUploadFile(company, plan, fileSizeBytes)
        
        assertTrue("Should allow file upload under storage limit", canUpload)
        assertNull("Should not have error message", message)
    }

    @Test
    fun `canUploadFile returns false when FREE plan storage would be exceeded`() {
        val company = createTestCompany(storageUsedBytes = 95 * 1024 * 1024) // 95MB used
        val plan = PlanDefaults.FREE // 100MB limit
        val fileSizeBytes = 10 * 1024 * 1024L // 10MB file (would exceed)
        
        val (canUpload, message) = FeatureFlags.canUploadFile(company, plan, fileSizeBytes)
        
        assertFalse("Should block file upload when storage would be exceeded", canUpload)
        assertNotNull("Should provide upgrade message", message)
        assertTrue("Message should mention storage limit", message!!.contains("MB"))
    }

    @Test
    fun `canUploadFile returns true for PRO plan with 5GB storage`() {
        val company = createTestCompany(storageUsedBytes = 1L * 1024 * 1024 * 1024) // 1GB used
        val plan = PlanDefaults.PRO // 5GB limit
        val fileSizeBytes = 500 * 1024 * 1024L // 500MB file
        
        val (canUpload, message) = FeatureFlags.canUploadFile(company, plan, fileSizeBytes)
        
        assertTrue("Should allow large file on PRO plan", canUpload)
        assertNull("Should not have error message", message)
    }

    // ========================================
    // BOOLEAN FEATURE TESTS
    // ========================================

    @Test
    fun `canUseBudgets returns false for FREE plan`() {
        val plan = PlanDefaults.FREE
        
        val (canUse, message) = FeatureFlags.canUseBudgets(plan)
        
        assertFalse("FREE plan should not have budgets", canUse)
        assertNotNull("Should provide upgrade message", message)
        assertTrue("Message should mention PRO requirement", message!!.contains("PRO"))
    }

    @Test
    fun `canUseBudgets returns true for PRO plan`() {
        val plan = PlanDefaults.PRO
        
        val (canUse, message) = FeatureFlags.canUseBudgets(plan)
        
        assertTrue("PRO plan should have budgets", canUse)
        assertNull("Should not have error message", message)
    }

    @Test
    fun `canUseBudgets returns true for ENTERPRISE plan`() {
        val plan = PlanDefaults.ENTERPRISE
        
        val (canUse, message) = FeatureFlags.canUseBudgets(plan)
        
        assertTrue("ENTERPRISE plan should have budgets", canUse)
        assertNull("Should not have error message", message)
    }

    @Test
    fun `canUseApprovalHierarchy returns false for FREE and PRO plans`() {
        val freePlan = PlanDefaults.FREE
        val proPlan = PlanDefaults.PRO
        
        val (canUseFree, messageFree) = FeatureFlags.canUseApprovalHierarchy(freePlan)
        val (canUsePro, messagePro) = FeatureFlags.canUseApprovalHierarchy(proPlan)
        
        assertFalse("FREE should not have approval hierarchy", canUseFree)
        assertFalse("PRO should not have approval hierarchy", canUsePro)
        assertNotNull("Should provide upgrade message for FREE", messageFree)
        assertNotNull("Should provide upgrade message for PRO", messagePro)
    }

    @Test
    fun `canUseApprovalHierarchy returns true for BUSINESS plan`() {
        val plan = PlanDefaults.BUSINESS
        
        val (canUse, message) = FeatureFlags.canUseApprovalHierarchy(plan)
        
        assertTrue("BUSINESS plan should have approval hierarchy", canUse)
        assertNull("Should not have error message", message)
    }

    @Test
    fun `canUseAnalytics returns false for FREE plan`() {
        val plan = PlanDefaults.FREE
        
        val (canUse, message) = FeatureFlags.canUseAnalytics(plan)
        
        assertFalse("FREE plan should not have analytics", canUse)
        assertNotNull("Should provide upgrade message", message)
    }

    @Test
    fun `canUseAnalytics returns true for PRO and higher plans`() {
        val proPlan = PlanDefaults.PRO
        val businessPlan = PlanDefaults.BUSINESS
        val enterprisePlan = PlanDefaults.ENTERPRISE
        
        val (canUsePro, _) = FeatureFlags.canUseAnalytics(proPlan)
        val (canUseBusiness, _) = FeatureFlags.canUseAnalytics(businessPlan)
        val (canUseEnterprise, _) = FeatureFlags.canUseAnalytics(enterprisePlan)
        
        assertTrue("PRO should have analytics", canUsePro)
        assertTrue("BUSINESS should have analytics", canUseBusiness)
        assertTrue("ENTERPRISE should have analytics", canUseEnterprise)
    }

    @Test
    fun `canExportData returns false for FREE and PRO plans`() {
        val freePlan = PlanDefaults.FREE
        val proPlan = PlanDefaults.PRO
        
        val (canUseFree, messageFree) = FeatureFlags.canExportData(freePlan)
        val (canUsePro, messagePro) = FeatureFlags.canExportData(proPlan)
        
        assertFalse("FREE should not have data export", canUseFree)
        assertFalse("PRO should not have data export", canUsePro)
        assertTrue("Message should mention BUSINESS", messageFree!!.contains("BUSINESS"))
    }

    @Test
    fun `canExportData returns true for BUSINESS and ENTERPRISE plans`() {
        val businessPlan = PlanDefaults.BUSINESS
        val enterprisePlan = PlanDefaults.ENTERPRISE
        
        val (canUseBusiness, _) = FeatureFlags.canExportData(businessPlan)
        val (canUseEnterprise, _) = FeatureFlags.canExportData(enterprisePlan)
        
        assertTrue("BUSINESS should have data export", canUseBusiness)
        assertTrue("ENTERPRISE should have data export", canUseEnterprise)
    }

    // ========================================
    // ENTERPRISE FEATURE TESTS
    // ========================================

    @Test
    fun `canUseSSO returns false for all plans except ENTERPRISE`() {
        val freePlan = PlanDefaults.FREE
        val proPlan = PlanDefaults.PRO
        val businessPlan = PlanDefaults.BUSINESS
        
        val (canUseFree, messageFree) = FeatureFlags.canUseSSO(freePlan)
        val (canUsePro, messagePro) = FeatureFlags.canUseSSO(proPlan)
        val (canUseBusiness, messageBusiness) = FeatureFlags.canUseSSO(businessPlan)
        
        assertFalse("FREE should not have SSO", canUseFree)
        assertFalse("PRO should not have SSO", canUsePro)
        assertFalse("BUSINESS should not have SSO", canUseBusiness)
        assertTrue("Message should mention ENTERPRISE", messageFree!!.contains("ENTERPRISE"))
    }

    @Test
    fun `canUseSSO returns true for ENTERPRISE plan`() {
        val plan = PlanDefaults.ENTERPRISE
        
        val (canUse, message) = FeatureFlags.canUseSSO(plan)
        
        assertTrue("ENTERPRISE should have SSO", canUse)
        assertNull("Should not have error message", message)
    }

    @Test
    fun `canUseAPI returns false for all plans except ENTERPRISE`() {
        val freePlan = PlanDefaults.FREE
        val proPlan = PlanDefaults.PRO
        val businessPlan = PlanDefaults.BUSINESS
        
        val (canUseFree, _) = FeatureFlags.canUseAPI(freePlan)
        val (canUsePro, _) = FeatureFlags.canUseAPI(proPlan)
        val (canUseBusiness, _) = FeatureFlags.canUseAPI(businessPlan)
        
        assertFalse("FREE should not have API access", canUseFree)
        assertFalse("PRO should not have API access", canUsePro)
        assertFalse("BUSINESS should not have API access", canUseBusiness)
    }

    @Test
    fun `canUseAPI returns true for ENTERPRISE plan`() {
        val plan = PlanDefaults.ENTERPRISE
        
        val (canUse, message) = FeatureFlags.canUseAPI(plan)
        
        assertTrue("ENTERPRISE should have API access", canUse)
        assertNull("Should not have error message", message)
    }

    @Test
    fun `canWhiteLabel returns false for all plans except ENTERPRISE`() {
        val freePlan = PlanDefaults.FREE
        val proPlan = PlanDefaults.PRO
        val businessPlan = PlanDefaults.BUSINESS
        
        val (canUseFree, messageFree) = FeatureFlags.canWhiteLabel(freePlan)
        val (canUsePro, _) = FeatureFlags.canWhiteLabel(proPlan)
        val (canUseBusiness, _) = FeatureFlags.canWhiteLabel(businessPlan)
        
        assertFalse("FREE should not have white-label", canUseFree)
        assertFalse("PRO should not have white-label", canUsePro)
        assertFalse("BUSINESS should not have white-label", canUseBusiness)
        assertTrue("Message should mention ENTERPRISE", messageFree!!.contains("ENTERPRISE"))
    }

    @Test
    fun `canWhiteLabel returns true for ENTERPRISE plan`() {
        val plan = PlanDefaults.ENTERPRISE
        
        val (canUse, message) = FeatureFlags.canWhiteLabel(plan)
        
        assertTrue("ENTERPRISE should have white-label", canUse)
        assertNull("Should not have error message", message)
    }

    @Test
    fun `canUseAuditLogs returns false for FREE and PRO plans`() {
        val freePlan = PlanDefaults.FREE
        val proPlan = PlanDefaults.PRO
        
        val (canUseFree, _) = FeatureFlags.canUseAuditLogs(freePlan)
        val (canUsePro, _) = FeatureFlags.canUseAuditLogs(proPlan)
        
        assertFalse("FREE should not have audit logs", canUseFree)
        assertFalse("PRO should not have audit logs", canUsePro)
    }

    @Test
    fun `canUseAuditLogs returns true for BUSINESS and ENTERPRISE plans`() {
        val businessPlan = PlanDefaults.BUSINESS
        val enterprisePlan = PlanDefaults.ENTERPRISE
        
        val (canUseBusiness, _) = FeatureFlags.canUseAuditLogs(businessPlan)
        val (canUseEnterprise, _) = FeatureFlags.canUseAuditLogs(enterprisePlan)
        
        assertTrue("BUSINESS should have audit logs", canUseBusiness)
        assertTrue("ENTERPRISE should have audit logs", canUseEnterprise)
    }

    // ========================================
    // UTILITY METHOD TESTS
    // ========================================

    @Test
    fun `getStorageUsagePercentage returns correct percentage`() {
        val company = createTestCompany(storageUsedBytes = 50 * 1024 * 1024) // 50MB
        val plan = PlanDefaults.FREE // 100MB limit
        
        val percentage = FeatureFlags.getStorageUsagePercentage(company, plan)
        
        assertEquals("Should return 50% usage", 50, percentage)
    }

    @Test
    fun `getStorageUsagePercentage returns 100 when limit exceeded`() {
        val company = createTestCompany(storageUsedBytes = 150 * 1024 * 1024) // 150MB
        val plan = PlanDefaults.FREE // 100MB limit
        
        val percentage = FeatureFlags.getStorageUsagePercentage(company, plan)
        
        assertEquals("Should cap at 100%", 100, percentage)
    }

    @Test
    fun `getStorageUsagePercentage returns 0 for unused storage`() {
        val company = createTestCompany(storageUsedBytes = 0)
        val plan = PlanDefaults.FREE
        
        val percentage = FeatureFlags.getStorageUsagePercentage(company, plan)
        
        assertEquals("Should return 0% for unused storage", 0, percentage)
    }

    @Test
    fun `getPhotoUsagePercentage returns correct percentage`() {
        val company = createTestCompany(photosUploadedThisMonth = 3)
        val plan = PlanDefaults.FREE // 5 photos/month
        
        val percentage = FeatureFlags.getPhotoUsagePercentage(company, plan)
        
        assertEquals("Should return 60% usage", 60, percentage)
    }

    @Test
    fun `getPhotoUsagePercentage returns 0 for unlimited plan`() {
        val company = createTestCompany(photosUploadedThisMonth = 500)
        val plan = PlanDefaults.ENTERPRISE // Unlimited
        
        val percentage = FeatureFlags.getPhotoUsagePercentage(company, plan)
        
        assertEquals("Should return 0% for unlimited plan", 0, percentage)
    }

    @Test
    fun `getSupportLevel returns correct level for each plan`() {
        val freePlan = PlanDefaults.FREE
        val proPlan = PlanDefaults.PRO
        val businessPlan = PlanDefaults.BUSINESS
        val enterprisePlan = PlanDefaults.ENTERPRISE
        
        val freeLevel = FeatureFlags.getSupportLevel(freePlan)
        val proLevel = FeatureFlags.getSupportLevel(proPlan)
        val businessLevel = FeatureFlags.getSupportLevel(businessPlan)
        val enterpriseLevel = FeatureFlags.getSupportLevel(enterprisePlan)
        
        assertEquals("FREE should have standard support", "standard", freeLevel)
        assertEquals("PRO should have priority support", "priority", proLevel)
        assertEquals("BUSINESS should have dedicated support", "dedicated", businessLevel)
        assertEquals("ENTERPRISE should have dedicated_manager support", "dedicated_manager", enterpriseLevel)
    }
}
