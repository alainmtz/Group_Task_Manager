package com.alainmtz.work_group_tasks.domain.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class Earning(
    val id: String = "",
    val userId: String = "",
    val subtaskId: String = "",
    val taskId: String = "",
    val subtaskTitle: String = "",
    val amount: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now(),
    val status: String = "pending", // "pending" or "confirmed"
    val creatorId: String = "",
    val companyId: String? = null
) {
    companion object {
        fun fromFirestore(doc: DocumentSnapshot): Earning {
            return Earning(
                id = doc.id,
                userId = doc.getString("userId") ?: "",
                subtaskId = doc.getString("subtaskId") ?: "",
                taskId = doc.getString("taskId") ?: "",
                subtaskTitle = doc.getString("subtaskTitle") ?: "",
                amount = doc.getDouble("amount") ?: 0.0,
                timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                status = doc.getString("status") ?: "pending",
                creatorId = doc.getString("creatorId") ?: "",
                companyId = doc.getString("companyId")
            )
        }
    }
}

data class PaidOutDetail(
    val earning: Earning,
    val userName: String = "",
    val userPhotoUrl: String? = null
)