package com.alainmtz.work_group_tasks.domain.models

import android.os.Parcelable
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.parcelize.Parcelize

@Parcelize
data class Group(
    val id: String,
    val name: String,
    val creatorId: String,
    val memberIds: List<String>,
    val code: String = "",
    val companyId: String? = null
) : Parcelable {
    companion object {
        fun fromFirestore(doc: DocumentSnapshot): Group {
            val data = doc.data!!
            return Group(
                id = doc.id,
                name = data["name"] as? String ?: "",
                creatorId = data["creatorId"] as? String ?: "",
                memberIds = data["memberIds"] as? List<String> ?: emptyList(),
                code = data["code"] as? String ?: "",
                companyId = data["companyId"] as? String
            )
        }
    }

    fun toFirestore(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "creatorId" to creatorId,
            "memberIds" to memberIds,
            "code" to code,
            "companyId" to companyId
        )
    }
}