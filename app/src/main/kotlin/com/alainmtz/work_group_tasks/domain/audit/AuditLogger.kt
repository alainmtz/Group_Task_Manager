package com.alainmtz.work_group_tasks.domain.audit

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Central audit logging system for enterprise compliance and debugging
 * Logs are stored both locally (logcat) and in Firestore for enterprise plans
 */
class AuditLogger(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "AuditLogger"
        private const val AUDIT_COLLECTION = "auditLogs"
    }

    /**
     * Log an audit event
     * - Always logs to logcat
     * - Stores in Firestore for enterprise plans (async, best-effort)
     */
    fun log(event: AuditEvent) {
        // Always log locally with appropriate level
        logToLogcat(event)
        
        // Store in Firestore for enterprise plans (async, don't block)
        storeInFirestore(event)
    }

    /**
     * Log multiple events (e.g., from a complex operation)
     */
    fun logBatch(events: List<AuditEvent>) {
        events.forEach { log(it) }
        
        // Log summary
        val summary = generateBatchSummary(events)
        Log.i(TAG, "Batch operation summary: $summary")
    }

    /**
     * Log operation result with full audit trail
     */
    fun logOperationResult(result: OperationResult) {
        logBatch(result.events)
        
        val summaryLog = buildString {
            appendLine("=== Operation Summary ===")
            appendLine("Total: ${result.summary.totalOperations}")
            appendLine("Success: ${result.summary.successfulOperations}")
            appendLine("Failed: ${result.summary.failedOperations}")
            appendLine("Skipped: ${result.summary.skippedOperations}")
            appendLine("Critical Success: ${result.summary.criticalOperationSuccess}")
            
            // Performance metrics
            if (result.summary.totalDurationMs > 0) {
                appendLine("Duration: ${result.summary.totalDurationMs}ms")
            }
            if (result.summary.resourcesAffected > 0) {
                appendLine("Resources Affected: ${result.summary.resourcesAffected}")
            }
            
            // Context information from first event
            result.events.firstOrNull()?.context?.let { ctx ->
                appendLine("\n📋 Context:")
                ctx.companyId?.let { appendLine("  Company: $it") }
                ctx.companyPlan?.let { appendLine("  Plan: $it") }
                ctx.sourceScreen?.let { appendLine("  Screen: $it") }
                ctx.triggerType?.let { appendLine("  Trigger: $it") }
            }
            
            if (result.hasPartialFailures()) {
                appendLine("\n⚠️ Partial failures detected:")
                result.getFailedOperations().forEach { event ->
                    val duration = event.metrics?.durationMs?.let { " (${it}ms)" } ?: ""
                    appendLine("  - ${event.action} on ${event.resource}$duration: ${event.errorDetails?.errorMessage}")
                }
            }
        }
        
        if (result.summary.criticalOperationSuccess) {
            Log.i(TAG, summaryLog)
        } else {
            Log.e(TAG, summaryLog)
        }
    }

    private fun logToLogcat(event: AuditEvent) {
        val message = formatEventMessage(event)
        
        when (event.status) {
            AuditStatus.SUCCESS -> Log.i(TAG, message)
            AuditStatus.PARTIAL_SUCCESS -> Log.w(TAG, message)
            AuditStatus.FAILED -> Log.e(TAG, message)
            AuditStatus.PERMISSION_DENIED -> Log.w(TAG, message)
            AuditStatus.SKIPPED -> Log.d(TAG, message)
        }
    }

    private fun formatEventMessage(event: AuditEvent): String {
        return buildString {
            append("[${event.eventId}] ")
            append("${event.action} on ${event.resource}(${event.resourceId}) ")
            append("by user ${event.userId} ")
            append("-> ${event.status}")
            
            // Add metrics if present
            event.metrics?.let { metrics ->
                append(" | ${metrics.durationMs}ms")
                if (metrics.resourcesAffected > 0) {
                    append(", ${metrics.resourcesAffected} resources")
                }
                if (metrics.retryCount > 0) {
                    append(", ${metrics.retryCount} retries")
                }
            }
            
            if (event.metadata.isNotEmpty()) {
                append(" | metadata: ${event.metadata}")
            }
            
            event.errorDetails?.let { error ->
                append(" | error: ${error.errorType} - ${error.errorMessage}")
            }
        }
    }

    private fun generateBatchSummary(events: List<AuditEvent>): String {
        val statusCounts = events.groupBy { it.status }.mapValues { it.value.size }
        return "Total: ${events.size}, Success: ${statusCounts[AuditStatus.SUCCESS] ?: 0}, " +
               "Failed: ${statusCounts[AuditStatus.FAILED] ?: 0}, " +
               "Partial: ${statusCounts[AuditStatus.PARTIAL_SUCCESS] ?: 0}, " +
               "PermDenied: ${statusCounts[AuditStatus.PERMISSION_DENIED] ?: 0}"
    }

    private fun storeInFirestore(event: AuditEvent) {
        try {
            // Best-effort async storage - don't wait or throw
            db.collection(AUDIT_COLLECTION)
                .add(mapOf(
                    "eventId" to event.eventId,
                    "timestamp" to event.timestamp,
                    "userId" to event.userId,
                    "action" to event.action.name,
                    "resource" to event.resource.name,
                    "resourceId" to event.resourceId,
                    "status" to event.status.name,
                    "metadata" to event.metadata,
                    "metrics" to event.metrics?.let {
                        mapOf(
                            "durationMs" to it.durationMs,
                            "retryCount" to it.retryCount,
                            "resourcesAffected" to it.resourcesAffected,
                            "dataSize" to it.dataSize
                        )
                    },
                    "context" to event.context?.let {
                        mapOf(
                            "companyId" to it.companyId,
                            "companyPlan" to it.companyPlan,
                            "deviceInfo" to it.deviceInfo,
                            "appVersion" to it.appVersion,
                            "sourceScreen" to it.sourceScreen,
                            "triggerType" to it.triggerType
                        )
                    },
                    "errorDetails" to event.errorDetails?.let {
                        mapOf(
                            "errorType" to it.errorType,
                            "errorMessage" to it.errorMessage,
                            "canRetry" to it.canRetry,
                            "affectedOperations" to it.affectedOperations
                        )
                    }
                ))
        } catch (e: Exception) {
            // Don't fail the operation if audit storage fails
            Log.w(TAG, "Could not store audit log in Firestore: ${e.message}")
        }
    }
}
