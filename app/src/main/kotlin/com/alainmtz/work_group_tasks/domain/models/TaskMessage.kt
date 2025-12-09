package com.alainmtz.work_group_tasks.domain.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

data class TaskMessage(
    val id: String,
    val text: String,
    val authorId: String,
    val authorName: String,
    val createdAt: Date,
    val status: String = "sent" // "sending", "sent", "delivered", "read"
) {
    companion object {
        fun fromFirestore(doc: DocumentSnapshot): TaskMessage {
            val data = doc.data!!
            return TaskMessage(
                id = doc.id,
                text = data["text"] as? String ?: "",
                authorId = data["authorId"] as? String ?: "",
                authorName = data["authorName"] as? String ?: "Unknown",
                createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                status = data["status"] as? String ?: "sent"
            )
        }
    }

    fun toFirestore(): Map<String, Any> {
        return mapOf(
            "text" to text,
            "authorId" to authorId,
            "authorName" to authorName,
            "createdAt" to Timestamp(createdAt),
            "status" to status
        )
    }
}
