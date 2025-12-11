package com.alainmtz.work_group_tasks.domain.audit

import java.util.Date

/**
 * Represents an auditable event in the system
 * Used for compliance, debugging, and analytics in enterprise plans
 */
data class AuditEvent(
    val eventId: String = generateEventId(),
    val timestamp: Date = Date(),
    val userId: String,
    val action: AuditAction,
    val resource: AuditResource,
    val resourceId: String,
    val status: AuditStatus,
    val metadata: Map<String, Any> = emptyMap(),
    val errorDetails: ErrorDetails? = null,
    val metrics: OperationMetrics? = null,
    val context: OperationContext? = null
) {
    companion object {
        private fun generateEventId(): String {
            return "${System.currentTimeMillis()}-${(Math.random() * 10000).toInt()}"
        }
    }
}

/**
 * Performance and resource metrics for an operation
 */
data class OperationMetrics(
    val durationMs: Long,
    val retryCount: Int = 0,
    val resourcesAffected: Int = 0,
    val dataSize: Long? = null // bytes, if applicable
)

/**
 * Additional context about the operation
 */
data class OperationContext(
    val companyId: String?,
    val companyPlan: String? = null, // FREE, PRO, BUSINESS, ENTERPRISE
    val deviceInfo: String? = null,
    val appVersion: String? = null,
    val sourceScreen: String? = null,
    val triggerType: String? = null // USER_ACTION, AUTOMATIC, SCHEDULED
)

enum class AuditAction {
    GROUP_DELETE,
    GROUP_LEAVE,
    MEMBER_REMOVE,
    MEMBER_ADD,
    TASK_DELETE,
    CHAT_DELETE,
    COUNTER_UPDATE
}

enum class AuditResource {
    GROUP,
    CHAT_THREAD,
    TASK,
    COMPANY_COUNTER
}

enum class AuditStatus {
    SUCCESS,
    PARTIAL_SUCCESS, // Some operations succeeded, others failed
    FAILED,
    PERMISSION_DENIED,
    SKIPPED
}

data class ErrorDetails(
    val errorType: String,
    val errorMessage: String,
    val canRetry: Boolean = false,
    val affectedOperations: List<String> = emptyList()
)

/**
 * Result of a multi-step operation with audit trail
 */
data class OperationResult(
    val success: Boolean,
    val events: List<AuditEvent>,
    val summary: OperationSummary
) {
    fun hasPartialFailures(): Boolean = events.any { 
        it.status == AuditStatus.FAILED || it.status == AuditStatus.PERMISSION_DENIED 
    }
    
    fun getFailedOperations(): List<AuditEvent> = events.filter {
        it.status == AuditStatus.FAILED || it.status == AuditStatus.PERMISSION_DENIED
    }
}

data class OperationSummary(
    val totalOperations: Int,
    val successfulOperations: Int,
    val failedOperations: Int,
    val skippedOperations: Int,
    val criticalOperationSuccess: Boolean, // e.g., group deletion succeeded
    val totalDurationMs: Long = 0,
    val resourcesAffected: Int = 0
)
