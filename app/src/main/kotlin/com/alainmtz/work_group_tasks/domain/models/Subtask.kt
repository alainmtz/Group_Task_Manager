package com.alainmtz.work_group_tasks.domain.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.parcelize.Parcelize
import java.util.Date

enum class SubtaskStatus {
    PENDING, REVIEW, COMPLETED
}

@Parcelize
data class Subtask(
    val id: String,
    val title: String,
    val description: String? = null,
    val parentTaskId: String,
    val assignedUserIds: List<String>,
    val creatorId: String,
    val status: SubtaskStatus = SubtaskStatus.PENDING,
    val confirmationImageUrl: String? = null,
    val assignments: Map<String, String> = emptyMap(),
    val budget: Double? = null,
    val bids: List<SubtaskBid> = emptyList(),
    val dueDate: Date? = null,
    val postponementRequests: List<PostponementRequest> = emptyList()
) : Parcelable {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromFirestore(doc: DocumentSnapshot): Subtask {
            val data = doc.data!!
            val assignedIds = data["assignedUserIds"] as? List<String> ?: emptyList()

            val assignmentsMap = (data["assignments"] as? Map<String, String>)
                ?: assignedIds.associateWith { "accepted" }

            val bidsList = (data["bids"] as? List<Map<String, Any>>)?.map { SubtaskBid.fromMap(it) } ?: emptyList()

            val postponementRequestsList = (data["postponementRequests"] as? List<Map<String, Any>>)?.map { PostponementRequest.fromMap(it) } ?: emptyList()

            return Subtask(
                id = doc.id,
                title = data["title"] as? String ?: "",
                description = data["description"] as? String,
                status = SubtaskStatus.valueOf((data["status"] as? String ?: "PENDING").uppercase()),
                parentTaskId = data["parentTaskId"] as? String ?: "",
                assignedUserIds = assignedIds,
                creatorId = data["creatorId"] as? String ?: "",
                confirmationImageUrl = data["confirmationImageUrl"] as? String,
                assignments = assignmentsMap,
                budget = (data["budget"] as? Number)?.toDouble(),
                bids = bidsList,
                dueDate = (data["dueDate" ] as? Timestamp)?.toDate(),
                postponementRequests = postponementRequestsList
            )
        }
    }

    fun toFirestore(): Map<String, Any?> {
        return mapOf(
            "title" to title,
            "description" to description,
            "status" to status.name.lowercase(),
            "parentTaskId" to parentTaskId,
            "assignedUserIds" to assignedUserIds,
            "creatorId" to creatorId,
            "confirmationImageUrl" to confirmationImageUrl,
            "assignments" to assignments,
            "budget" to budget,
            "bids" to bids.map { it.toMap() },
            "dueDate" to dueDate?.let { Timestamp(it) },
            "postponementRequests" to postponementRequests.map { it.toMap() }
        )
    }
}

@Parcelize
data class SubtaskBid(
    val userId: String,
    val amount: Double,
    val status: String = "pending" // "pending", "accepted", "rejected"
) : Parcelable {
    companion object {
        fun fromMap(map: Map<String, Any>): SubtaskBid {
            return SubtaskBid(
                userId = map["userId"] as String,
                amount = (map["amount"] as Number).toDouble(),
                status = map["status"] as? String ?: "pending"
            )
        }
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "amount" to amount,
            "status" to status
        )
    }
}

@Parcelize
data class PostponementRequest(
    val userId: String,
    val requestedDate: Date,
    val reason: String,
    val status: String = "pending" // "pending", "accepted", "rejected"
) : Parcelable {
    companion object {
        fun fromMap(map: Map<String, Any>): PostponementRequest {
            return PostponementRequest(
                userId = map["userId"] as String,
                requestedDate = (map["requestedDate"] as Timestamp).toDate(),
                reason = map["reason"] as String,
                status = map["status"] as? String ?: "pending"
            )
        }
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "requestedDate" to Timestamp(requestedDate),
            "reason" to reason,
            "status" to status
        )
    }
}