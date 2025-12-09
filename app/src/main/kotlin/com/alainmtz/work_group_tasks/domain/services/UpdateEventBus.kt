package com.alainmtz.work_group_tasks.domain.services

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class UpdateEvent(
    val eventType: String,
    val taskId: String? = null,
    val subtaskId: String? = null,
    val userId: String? = null,
    val groupId: String? = null,
    val chatThreadId: String? = null,
    val data: Map<String, String> = emptyMap()
)

/**
 * Event types for reactive updates across the app
 */
object EventType {
    // Task events
    const val TASK_CREATED = "TASK_CREATED"
    const val TASK_UPDATED = "TASK_UPDATED"
    const val TASK_DELETED = "TASK_DELETED"
    const val TASK_STATUS_CHANGED = "TASK_STATUS_CHANGED"
    const val TASK_ASSIGNMENT_CHANGED = "TASK_ASSIGNMENT_CHANGED"
    
    // Subtask events
    const val SUBTASK_CREATED = "SUBTASK_CREATED"
    const val SUBTASK_UPDATED = "SUBTASK_UPDATED"
    const val SUBTASK_DELETED = "SUBTASK_DELETED"
    const val SUBTASK_STATUS_CHANGED = "SUBTASK_STATUS_CHANGED"
    const val SUBTASK_ASSIGNMENT_CHANGED = "SUBTASK_ASSIGNMENT_CHANGED"
    const val SUBTASK_COMPLETION_APPROVED = "SUBTASK_COMPLETION_APPROVED"
    const val SUBTASK_COMPLETION_REJECTED = "SUBTASK_COMPLETION_REJECTED"
    
    // Budget events
    const val BUDGET_STATUS_CHANGED = "BUDGET_STATUS_CHANGED"
    const val BUDGET_DISTRIBUTED = "BUDGET_DISTRIBUTED"
    const val EARNING_STATUS_CHANGED = "EARNING_STATUS_CHANGED"
    
    // Bid events
    const val BID_PLACED = "BID_PLACED"
    const val BID_ACCEPTED = "BID_ACCEPTED"
    const val BID_REJECTED = "BID_REJECTED"
    
    // Group events
    const val GROUP_CREATED = "GROUP_CREATED"
    const val GROUP_UPDATED = "GROUP_UPDATED"
    const val GROUP_DELETED = "GROUP_DELETED"
    const val GROUP_MEMBER_ADDED = "GROUP_MEMBER_ADDED"
    const val GROUP_MEMBER_REMOVED = "GROUP_MEMBER_REMOVED"
    
    // Chat events
    const val MESSAGE_RECEIVED = "MESSAGE_RECEIVED"
    const val CHAT_THREAD_UPDATED = "CHAT_THREAD_UPDATED"
    
    // User events
    const val USER_PROFILE_UPDATED = "USER_PROFILE_UPDATED"
}

object UpdateEventBus {
    private val _events = MutableSharedFlow<UpdateEvent>(
        replay = 0,
        extraBufferCapacity = 20 // Increased buffer for high-traffic scenarios
    )
    val events: SharedFlow<UpdateEvent> = _events.asSharedFlow()
    
    suspend fun emit(event: UpdateEvent) {
        _events.emit(event)
    }
    
    fun tryEmit(event: UpdateEvent): Boolean {
        return _events.tryEmit(event)
    }
}
