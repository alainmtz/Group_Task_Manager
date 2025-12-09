package com.alainmtz.work_group_tasks.domain.services

import com.alainmtz.work_group_tasks.domain.models.NotificationType
import com.alainmtz.work_group_tasks.domain.models.PostponementRequest
import com.alainmtz.work_group_tasks.domain.models.Subtask
import com.alainmtz.work_group_tasks.domain.models.SubtaskBid
import com.alainmtz.work_group_tasks.domain.models.SubtaskStatus
import com.alainmtz.work_group_tasks.domain.models.Task
import com.alainmtz.work_group_tasks.domain.models.TaskPriority
import com.alainmtz.work_group_tasks.domain.models.TaskStatus
import com.alainmtz.work_group_tasks.utils.snapshots
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestoreService {

    private val db = FirebaseFirestore.getInstance()
    private val notificationService = NotificationService() // Our wrapper
    private val tasksCollectionPath = "tasks"
    private val subtasksCollectionPath = "subtasks"

    fun getTasksStream(
        userId: String,
        priority: TaskPriority? = null,
        startDate: Date? = null,
        endDate: Date? = null
    ): Flow<List<Task>> {
        var query: Query = db.collection(tasksCollectionPath)
            .whereArrayContains("assignedUserIds", userId)

        if (priority != null) {
            query = query.whereEqualTo("priority", priority.name.lowercase())
        }
        if (startDate != null) {
            query = query.whereGreaterThanOrEqualTo("dueDate", startDate)
        }
        if (endDate != null) {
            val inclusiveEndDate = Date(endDate.time + 86400000) // Add one day
            query = query.whereLessThan("dueDate", inclusiveEndDate)
        }

        query = if (startDate != null || endDate != null) {
            query.orderBy("dueDate").orderBy("priority", Query.Direction.DESCENDING)
        } else {
            query.orderBy("priority", Query.Direction.DESCENDING).orderBy("dueDate")
        }

        return query.snapshots().map { snapshot ->
            snapshot.toObjects<Task>()
        }
    }

    suspend fun getTaskById(taskId: String): Task? {
        return try {
            val doc = db.collection(tasksCollectionPath).document(taskId).get().await()
            doc.toObject(Task::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addTask(task: Task): DocumentReference? {
        val docRef = db.collection(tasksCollectionPath).add(task.toFirestore()).await()

        val assignedUsers = task.assignedUserIds.filter { it != task.creatorId }
        if (assignedUsers.isNotEmpty()) {
            notificationService.notifyTaskMembers(
                memberIds = assignedUsers,
                title = "New task assigned",
                body = "You have been assigned to: ${task.title}",
                type = NotificationType.TASK_ASSIGNED,
                data = mapOf("taskId" to docRef.id, "groupId" to task.groupId)
            )
        }
        return docRef
    }

    suspend fun updateTask(task: Task) {
        db.collection(tasksCollectionPath).document(task.id).update(task.toFirestore()).await()

        val assignedUsers = task.assignedUserIds.filter { it != task.creatorId }
        if (assignedUsers.isNotEmpty()) {
            notificationService.notifyTaskMembers(
                memberIds = assignedUsers,
                title = "Task updated",
                body = "${task.title} has been updated",
                type = NotificationType.TASK_UPDATED,
                data = mapOf("taskId" to task.id, "groupId" to task.groupId)
            )
        }
    }

    suspend fun updateTaskStatus(taskId: String, newStatus: TaskStatus, updatedByUserId: String) {
        db.collection(tasksCollectionPath).document(taskId).update(
            mapOf(
                "status" to newStatus.name.lowercase(),
                "lastUpdatedBy" to updatedByUserId
            )
        ).await()
    }

    suspend fun deleteTask(taskId: String) {
        db.collection(tasksCollectionPath).document(taskId).delete().await()
    }

    // --- Subtask Methods ---

    fun getSubtasksStream(parentTaskId: String): Flow<List<Subtask>> {
        return db.collection(subtasksCollectionPath)
            .whereEqualTo("parentTaskId", parentTaskId)
            .snapshots()
            .map { snapshot -> snapshot.toObjects<Subtask>() }
    }

    suspend fun addSubtask(subtask: Subtask): DocumentReference? {
        val docRef = db.collection(subtasksCollectionPath).add(subtask.toFirestore()).await()

        val taskDoc = db.collection(tasksCollectionPath).document(subtask.parentTaskId).get().await()
        if(taskDoc.exists()){
            val task = taskDoc.toObject(Task::class.java)
            if(task != null){
                 val assignedUsers = task.assignedUserIds.filter { it != subtask.creatorId }
                if (assignedUsers.isNotEmpty()) {
                    notificationService.notifyTaskMembers(
                        memberIds = assignedUsers,
                        title = "New Subtask Added",
                        body = "A new subtask \"${subtask.title}\" was added to \"${task.title}\"",
                        type = NotificationType.TASK_UPDATED, 
                        data = mapOf("taskId" to task.id, "groupId" to task.groupId)
                    )
                }
            }
        }

        return docRef
    }

    suspend fun updateSubtaskStatus(
        subtaskId: String,
        newStatus: SubtaskStatus,
        updatedByUserId: String,
        confirmationImageUrl: String? = null
    ) {
        val data = mutableMapOf<String, Any>(
            "status" to newStatus.name.lowercase(),
            "lastUpdatedBy" to updatedByUserId
        )

        if (confirmationImageUrl != null) {
            data["confirmationImageUrl"] = confirmationImageUrl
        }

        db.collection(subtasksCollectionPath).document(subtaskId).update(data).await()
    }
}
