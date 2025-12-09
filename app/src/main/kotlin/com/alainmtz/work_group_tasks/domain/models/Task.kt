package com.alainmtz.work_group_tasks.domain.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.parcelize.Parcelize
import java.util.Date

enum class TaskStatus {
    PENDING,
    IN_REVIEW,
    COMPLETED
}

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH
}

@Parcelize
data class Task(
    val id: String,
    val title: String,
    val description: String,
    val dueDate: Date,
    val creatorId: String,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val assignedUserIds: List<String> = emptyList(),
    val groupId: String? = null,
    val totalBudget: Double? = null,
    val postponementRequests: List<PostponementRequest> = emptyList()
) : Parcelable {
    companion object {
        private inline fun <reified T : Enum<T>> safeEnumValueOf(value: String?, default: T): T {
            if (value == null) return default
            return try {
                enumValueOf<T>(value.uppercase())
            } catch (e: IllegalArgumentException) {
                try {
                    // Try to handle "ClassName.CONSTANT" format
                    enumValueOf<T>(value.substringAfterLast(".").uppercase())
                } catch (e2: IllegalArgumentException) {
                    default
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun fromFirestore(doc: DocumentSnapshot): Task {
            val data = doc.data!!
            return Task(
                id = doc.id,
                title = data["title"] as? String ?: "",
                description = data["description"] as? String ?: "",
                dueDate = (data["dueDate"] as Timestamp).toDate(),
                creatorId = data["creatorId"] as? String ?: "",
                priority = safeEnumValueOf(data["priority"] as? String, TaskPriority.MEDIUM),
                status = safeEnumValueOf(data["status"] as? String, TaskStatus.PENDING),
                assignedUserIds = data["assignedUserIds"] as? List<String> ?: emptyList(),
                groupId = data["groupId"] as? String,
                totalBudget = (data["totalBudget"] as? Number)?.toDouble(),
                postponementRequests = (data["postponementRequests"] as? List<Map<String, Any>>)?.map { PostponementRequest.fromMap(it) } ?: emptyList()
            )
        }
    }

    fun toFirestore(): Map<String, Any?> {
        return mapOf(
            "title" to title,
            "description" to description,
            "dueDate" to Timestamp(dueDate),
            "creatorId" to creatorId,
            "priority" to priority.name.lowercase(),
            "status" to status.name.lowercase(),
            "assignedUserIds" to assignedUserIds,
            "groupId" to groupId,
            "totalBudget" to totalBudget,
            "postponementRequests" to postponementRequests.map { it.toMap() }
        )
    }
}