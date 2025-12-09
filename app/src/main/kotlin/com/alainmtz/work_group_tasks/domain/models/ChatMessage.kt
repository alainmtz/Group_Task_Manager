package com.alainmtz.work_group_tasks.domain.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

data class Attachment(
    val url: String,
    val type: String, // "image", "file"
    val name: String
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val createdAt: Date,
    val readBy: List<String> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val status: String = "sent" // "sending", "sent", "delivered", "read"
) {
    companion object {
        fun fromFirestore(doc: DocumentSnapshot): ChatMessage {
            val data = doc.data!!
            val attachmentsList = (data["attachments"] as? List<Map<String, String>>)?.map {
                Attachment(
                    url = it["url"] ?: "",
                    type = it["type"] ?: "file",
                    name = it["name"] ?: ""
                )
            } ?: emptyList()

            return ChatMessage(
                id = doc.id,
                senderId = data["senderId"] as? String ?: "",
                text = data["text"] as? String ?: "",
                createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                readBy = (data["readBy"] as? List<String>) ?: emptyList(),
                attachments = attachmentsList,
                status = data["status"] as? String ?: "sent"
            )
        }
    }

    fun toFirestore(): Map<String, Any> {
        return mapOf(
            "senderId" to senderId,
            "text" to text,
            "createdAt" to Timestamp(createdAt),
            "readBy" to readBy,
            "attachments" to attachments.map {
                mapOf(
                    "url" to it.url,
                    "type" to it.type,
                    "name" to it.name
                )
            },
            "status" to status
        )
    }
}
