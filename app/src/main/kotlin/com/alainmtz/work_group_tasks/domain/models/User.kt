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
    val photoUrl: String? = null
) : Parcelable {
    companion object {
        fun fromFirestore(doc: DocumentSnapshot): User {
            val data = doc.data!!
            return User(
                id = doc.id,
                email = data["email"] as? String ?: "",
                name = data["name"] as? String,
                phoneNumber = data["phoneNumber"] as? String,
                photoUrl = data["photoUrl"] as? String
            )
        }
    }

    fun toFirestore(): Map<String, Any?> {
        return mapOf(
            "email" to email,
            "name" to name,
            "phoneNumber" to phoneNumber,
            "photoUrl" to photoUrl
        )
    }
}