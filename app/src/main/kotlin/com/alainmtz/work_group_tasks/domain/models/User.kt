package com.alainmtz.work_group_tasks.domain.models

import android.os.Parcelable
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String,
    val email: String,
    val name: String? = null,
    val phoneNumber: String? = null,
    val photoUrl: String? = null,
    val companyId: String? = null,
    val role: CompanyRole? = null,
    val storageUsedBytes: Long = 0L,
    val uploadCountThisMonth: Int = 0
) : Parcelable {
    companion object {
        fun fromFirestore(doc: DocumentSnapshot): User {
            val data = doc.data!!
            val roleString = data["role"] as? String
            val companyRole = when (roleString?.uppercase()) {
                "OWNER" -> CompanyRole.OWNER
                "ADMIN" -> CompanyRole.ADMIN
                "MEMBER" -> CompanyRole.MEMBER
                else -> null
            }
            return User(
                id = doc.id,
                email = data["email"] as? String ?: "",
                name = data["name"] as? String,
                phoneNumber = data["phoneNumber"] as? String,
                photoUrl = data["photoUrl"] as? String,
                companyId = data["companyId"] as? String,
                role = companyRole,
                storageUsedBytes = (data["storageUsedBytes"] as? Number)?.toLong() ?: 0L,
                uploadCountThisMonth = (data["uploadCountThisMonth"] as? Number)?.toInt() ?: 0
            )
        }
    }

    fun toFirestore(): Map<String, Any?> {
        return mapOf(
            "email" to email,
            "name" to name,
            "phoneNumber" to phoneNumber,
            "photoUrl" to photoUrl,
            "companyId" to companyId,
            "role" to role,
            "storageUsedBytes" to storageUsedBytes,
            "uploadCountThisMonth" to uploadCountThisMonth
        )
    }
}