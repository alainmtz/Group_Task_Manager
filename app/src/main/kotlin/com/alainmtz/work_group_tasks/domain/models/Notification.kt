package com.alainmtz.work_group_tasks.domain.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.parcelize.Parcelize
import java.util.Date

enum class NotificationType(val value: String) {
    TASK_ASSIGNED("NotificationType.taskAssigned"),
    TASK_UPDATED("NotificationType.taskUpdated"),
    TASK_COMPLETED("NotificationType.taskCompleted"),
    TASK_DELETED("NotificationType.taskDeleted"),
    TASK_COMMENT("NotificationType.taskComment"),
    SUBTASK_COMPLETED("NotificationType.subtaskCompleted"),
    SUBTASK_DELETED("NotificationType.subtaskDeleted"),
    DEADLINE_APPROACHING("NotificationType.deadlineApproaching"),
    MESSAGE_RECEIVED("NotificationType.messageReceived"),
    BUDGET_PROPOSAL("NotificationType.budgetProposal"),
    MEMBER_ASSIGNED("NotificationType.memberAssigned"),
    DEADLINE_EXTENSION_REQUEST("NotificationType.deadlineExtensionRequest"),
    DEADLINE_EXTENSION_APPROVAL("NotificationType.deadlineExtensionApproval"),
    DEADLINE_EXTENSION_REJECTION("NotificationType.deadlineExtensionRejection");

    companion object {
        fun fromString(value: String): NotificationType {
            return values().find { it.value == value } ?: TASK_ASSIGNED
        }
    }
}

@Parcelize
data class Notification(
    val id: String,
    val userId: String,
    val title: String,
    val body: String,
    val type: NotificationType,
    val createdAt: Date,
    val isRead: Boolean = false,
    val data: Map<String, String> = emptyMap()
) : Parcelable {

    val relatedId: String? get() = data["taskId"]

    companion object {
        fun fromFirestore(doc: DocumentSnapshot): Notification {
            val data = doc.data!!
            return Notification(
                id = doc.id,
                userId = data["userId"] as String,
                title = data["title"] as String,
                body = data["body"] as String,
                type = NotificationType.fromString(data["type"] as String),
                createdAt = (data["createdAt"] as Timestamp).toDate(),
                isRead = data["isRead"] as? Boolean ?: false,
                data = (data["data"] as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap()
            )
        }
    }

    fun toFirestore(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "title" to title,
            "body" to body,
            "type" to type.value,
            "createdAt" to Timestamp(createdAt),
            "isRead" to isRead,
            "data" to data
        )
    }
}