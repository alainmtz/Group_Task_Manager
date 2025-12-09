package com.alainmtz.work_group_tasks.domain.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.parcelize.Parcelize
import java.util.Date

enum class ChatType {
    PRIVATE, GROUP, TASK, SUBTASK
}

@Parcelize
data class ChatThread(
    val id: String,
    val memberIds: List<String>,
    val lastMessage: String,
    val lastMessageTimestamp: Date,
    val type: ChatType,
    val title: String? = null,
    val taskId: String? = null,
    val subtaskId: String? = null,
    val groupId: String? = null,
    val unreadCounts: Map<String, Int> = emptyMap()
) : Parcelable {
    companion object {
        fun fromFirestore(doc: DocumentSnapshot): ChatThread {
            val data = doc.data!!
            return ChatThread(
                id = doc.id,
                memberIds = data["memberIds"] as List<String>,
                lastMessage = data["lastMessage"] as? String ?: "",
                lastMessageTimestamp = (data["lastMessageTimestamp"] as Timestamp).toDate(),
                type = ChatType.valueOf((data["type"] as String).uppercase()),
                title = data["title"] as? String,
                taskId = data["taskId"] as? String,
                subtaskId = data["subtaskId"] as? String,
                groupId = data["groupId"] as? String,
                unreadCounts = (data["unreadCounts"] as? Map<String, Long>)?.mapValues { it.value.toInt() } ?: emptyMap()
            )
        }
    }

    fun toFirestore(): Map<String, Any?> {
        return mapOf(
            "memberIds" to memberIds,
            "lastMessage" to lastMessage,
            "lastMessageTimestamp" to lastMessageTimestamp,
            "type" to type.name.lowercase(),
            "title" to title,
            "taskId" to taskId,
            "subtaskId" to subtaskId,
            "groupId" to groupId,
            "unreadCounts" to unreadCounts
        )
    }
}