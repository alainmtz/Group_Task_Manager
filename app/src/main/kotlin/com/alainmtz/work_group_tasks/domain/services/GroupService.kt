package com.alainmtz.work_group_tasks.domain.services

import com.alainmtz.work_group_tasks.domain.models.Group
import com.alainmtz.work_group_tasks.utils.snapshots
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GroupService {

    private val db = FirebaseFirestore.getInstance()
    private val collectionPath = "groups"

    suspend fun createGroup(groupName: String): String? {
        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: throw Exception("User must be logged in to create a group.")

        val newGroup = Group(
            id = "", // Firestore will generate the ID
            name = groupName,
            creatorId = currentUser.uid,
            memberIds = listOf(currentUser.uid)
        )

        val docRef = db.collection(collectionPath).add(newGroup.toFirestore()).await()
        return docRef.id
    }

    fun getGroupsStream(userId: String): Flow<List<Group>> {
        return db.collection(collectionPath)
            .whereArrayContains("memberIds", userId)
            .snapshots()
            .map { snapshot -> snapshot.toObjects<Group>() }
    }

    suspend fun addMemberToGroup(groupId: String, newMemberId: String) {
        db.collection(collectionPath).document(groupId).update(
            "memberIds", FieldValue.arrayUnion(newMemberId)
        ).await()
    }

    suspend fun removeMemberFromGroup(groupId: String, memberId: String) {
        db.collection(collectionPath).document(groupId).update(
            "memberIds", FieldValue.arrayRemove(memberId)
        ).await()
    }

    suspend fun updateGroup(groupId: String, newName: String) {
        db.collection(collectionPath).document(groupId).update("name", newName).await()
    }

    suspend fun deleteGroup(groupId: String) {
        db.collection(collectionPath).document(groupId).delete().await()
    }

    suspend fun getGroupById(groupId: String): Group? {
        return try {
            val doc = db.collection(collectionPath).document(groupId).get().await()
            doc.toObject(Group::class.java)
        } catch (e: Exception) {
            null
        }
    }
}