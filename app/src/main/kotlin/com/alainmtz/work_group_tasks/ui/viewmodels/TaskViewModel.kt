package com.alainmtz.work_group_tasks.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alainmtz.work_group_tasks.domain.models.Company
import com.alainmtz.work_group_tasks.domain.models.NotificationType
import com.alainmtz.work_group_tasks.domain.models.PendingUpload
import com.alainmtz.work_group_tasks.domain.models.PlanTier
import com.alainmtz.work_group_tasks.domain.models.UploadStatus
import com.alainmtz.work_group_tasks.domain.models.PostponementRequest
import com.alainmtz.work_group_tasks.domain.models.Subtask
import com.alainmtz.work_group_tasks.domain.models.SubtaskBid
import com.alainmtz.work_group_tasks.domain.models.SubtaskStatus
import com.alainmtz.work_group_tasks.domain.models.TaskMessage
import com.alainmtz.work_group_tasks.domain.models.Task
import com.alainmtz.work_group_tasks.domain.models.TaskPriority
import com.alainmtz.work_group_tasks.domain.models.TaskStatus
import com.alainmtz.work_group_tasks.domain.services.NotificationService
import com.alainmtz.work_group_tasks.domain.services.CompanyPlanProvider
import com.alainmtz.work_group_tasks.domain.services.FeatureFlags
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException
import java.util.Date

class TaskViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationService = NotificationService()
    private val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
    private val tasksCollection = db.collection("tasks")
    private val subtasksCollection = db.collection("subtasks")

    private var _allTasks = listOf<Task>() // Store all fetched tasks here

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _filterPriority = MutableStateFlow<TaskPriority?>(null)
    val filterPriority: StateFlow<TaskPriority?> = _filterPriority.asStateFlow()

    private val _filterDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val filterDateRange: StateFlow<Pair<Long, Long>?> = _filterDateRange.asStateFlow()

    private val _subtasks = MutableStateFlow<List<Subtask>>(emptyList())
    val subtasks: StateFlow<List<Subtask>> = _subtasks.asStateFlow()

    private val _messages = MutableStateFlow<List<TaskMessage>>(emptyList())
    val messages: StateFlow<List<TaskMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()
    
    private val _pendingUploads = MutableStateFlow<List<PendingUpload>>(emptyList())
    val pendingUploads: StateFlow<List<PendingUpload>> = _pendingUploads.asStateFlow()

    val currentUserId: String?
        get() = auth.currentUser?.uid

    init {
        fetchUserTasks()
    }
    
    private suspend fun calculateTotalBudget(taskId: String) {
        try {
            val subtasksSnapshot = subtasksCollection.whereEqualTo("parentTaskId", taskId).get().await()
            val totalBudget = subtasksSnapshot.documents.sumOf { it.getDouble("budget") ?: 0.0 }
            tasksCollection.document(taskId).update("totalBudget", totalBudget).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("TaskViewModel", "calculateTotalBudget: Offline or error, operation will sync later", e)
            // Firestore will automatically retry when connection is restored due to offline persistence
        }
    }

    fun rejectBid(subtask: Subtask, bid: SubtaskBid) {
        viewModelScope.launch {
            val subtaskRef = subtasksCollection.document(subtask.id)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(subtaskRef)
                val currentSubtask = Subtask.fromFirestore(snapshot)
                val updatedBids = currentSubtask.bids.map {
                    if (it.userId == bid.userId && it.amount == bid.amount) {
                        it.copy(status = "rejected")
                    } else {
                        it
                    }
                }
                transaction.update(subtaskRef, "bids", updatedBids.map { it.toMap() })
            }.await()
            
            // Send FCM event
            val taskDoc = tasksCollection.document(subtask.parentTaskId).get().await()
            val task = Task.fromFirestore(taskDoc)
            val allUserIds = (task.assignedUserIds + task.creatorId + subtask.assignedUserIds).distinct()
            notificationService.sendUpdateEvent(
                userIds = allUserIds,
                eventType = "BID_REJECTED",
                data = mapOf(
                    "taskId" to subtask.parentTaskId,
                    "subtaskId" to subtask.id,
                    "userId" to bid.userId
                )
            )
            
            fetchSubtasks(subtask.parentTaskId)
        }
    }

    fun updateSubtaskAssignments(subtaskId: String, userIds: List<String>, parentTaskId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get current subtask to compare assignments
                val subtaskDoc = subtasksCollection.document(subtaskId).get().await()
                val currentUserIds = (subtaskDoc.get("assignedUserIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val subtaskTitle = subtaskDoc.getString("title") ?: "Subtask"
                
                // Update assignments
                subtasksCollection.document(subtaskId).update("assignedUserIds", userIds).await()
                
                // Get current user name
                val currentUserDoc = db.collection("users").document(auth.currentUser?.uid ?: "").get().await()
                val currentUserName = currentUserDoc.getString("name") ?: "User"
                
                // Detect added members
                val addedUserIds = userIds.filter { it !in currentUserIds }
                for (userId in addedUserIds) {
                    try {
                        val userDoc = db.collection("users").document(userId).get().await()
                        val userName = userDoc.getString("name") ?: "User"
                        sendSystemMessage(parentTaskId, "👤 $currentUserName assigned $userName to: [$subtaskTitle](#$subtaskId)")
                    } catch (e: Exception) {
                        Log.e("TaskViewModel", "updateSubtaskAssignments: Error getting user name", e)
                    }
                }
                
                // Detect removed members
                val removedUserIds = currentUserIds.filter { it !in userIds }
                for (userId in removedUserIds) {
                    try {
                        val userDoc = db.collection("users").document(userId).get().await()
                        val userName = userDoc.getString("name") ?: "User"
                        sendSystemMessage(parentTaskId, "👤 $currentUserName removed $userName from: [$subtaskTitle](#$subtaskId)")
                    } catch (e: Exception) {
                        Log.e("TaskViewModel", "updateSubtaskAssignments: Error getting user name", e)
                    }
                }
                
                fetchSubtasks(parentTaskId)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchSubtasks(taskId: String) {
        viewModelScope.launch {
            try {
                Log.d("TaskViewModel", "fetchSubtasks: Starting for taskId $taskId")
                val snapshot = subtasksCollection
                    .whereEqualTo("parentTaskId", taskId)
                    .get()
                    .await()
                
                val fetchedSubtasks = snapshot.documents.map { Subtask.fromFirestore(it) }
                Log.d("TaskViewModel", "fetchSubtasks: Retrieved ${fetchedSubtasks.size} subtasks")
                _subtasks.value = fetchedSubtasks
                Log.d("TaskViewModel", "fetchSubtasks: Updated _subtasks StateFlow")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "fetchSubtasks: Error", e)
                _error.value = e.message
            }
        }
    }

    fun addSubtask(parentTaskId: String, title: String, description: String?, dueDate: Date?, budget: Double?) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(userId).get().await()
                val userName = userDoc.getString("name") ?: "User"
                
                val newSubtask = hashMapOf(
                    "title" to title,
                    "description" to description,
                    "parentTaskId" to parentTaskId,
                    "creatorId" to userId,
                    "status" to "pending",
                    "assignedUserIds" to emptyList<String>(),
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "dueDate" to dueDate?.let { com.google.firebase.Timestamp(it) },
                    "budget" to budget
                )
                
                val docRef = subtasksCollection.add(newSubtask).await()
                val subtaskId = docRef.id
                calculateTotalBudget(parentTaskId)
                
                // Post system message to chat
                val budgetText = if (budget != null) " with budget $${String.format("%.2f", budget)}" else ""
                sendSystemMessage(parentTaskId, "📼 $userName created subtask: [$title](#$subtaskId)$budgetText")
                
                // Send FCM event
                val taskDoc = tasksCollection.document(parentTaskId).get().await()
                val task = Task.fromFirestore(taskDoc)
                val allUserIds = (task.assignedUserIds + task.creatorId).distinct()
                notificationService.sendUpdateEvent(
                    userIds = allUserIds,
                    eventType = "SUBTASK_CREATED",
                    data = mapOf("taskId" to parentTaskId)
                )
                
                fetchSubtasks(parentTaskId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun completeSubtaskWithProof(subtask: Subtask, imageUri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Save image to local cache first
                val cacheDir = context.cacheDir
                val localFile = java.io.File(cacheDir, "pending_upload_${subtask.id}_${System.currentTimeMillis()}.jpg")
                
                try {
                    context.contentResolver.openInputStream(imageUri)?.use { input ->
                        localFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TaskViewModel", "Error caching image locally", e)
                    _error.value = "Failed to prepare image for upload. Please try again."
                    _isLoading.value = false
                    return@launch
                }
                
                // Add to pending uploads
                val pendingUpload = PendingUpload(
                    subtaskId = subtask.id,
                    subtaskTitle = subtask.title,
                    parentTaskId = subtask.parentTaskId,
                    imageUri = imageUri,
                    localCachePath = localFile.absolutePath,
                    status = UploadStatus.UPLOADING
                )
                _pendingUploads.value = _pendingUploads.value + pendingUpload
                
                // Attempt upload with retry logic
                uploadImageWithRetry(subtask, localFile, pendingUpload, maxRetries = 3)
                
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error in completeSubtaskWithProof", e)
                _error.value = "Failed to process image: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun uploadImageWithRetry(
        subtask: Subtask,
        localFile: java.io.File,
        pendingUpload: PendingUpload,
        maxRetries: Int = 3
    ) {
        var lastError: Exception? = null
        
        for (attempt in 0 until maxRetries) {
            try {
                Log.d("TaskViewModel", "Upload attempt ${attempt + 1} of $maxRetries for ${subtask.title}")
                
                val userDoc = db.collection("users").document(auth.currentUser?.uid ?: "").get().await()
                val userName = userDoc.getString("name") ?: "User"
                
                // Upload image to Firebase Storage
                val storageRef = storage.reference.child("task-evidence/${subtask.parentTaskId}/${subtask.id}/${java.util.UUID.randomUUID()}")
                val uploadTask = storageRef.putFile(android.net.Uri.fromFile(localFile)).await()
                val downloadUrl = storageRef.downloadUrl.await()

                // Update subtask with confirmation image URL
                subtasksCollection.document(subtask.id)
                    .update(
                        mapOf(
                            "status" to SubtaskStatus.REVIEW.name.lowercase(),
                            "confirmationImageUrl" to downloadUrl.toString()
                        )
                    )
                    .await()
                
                // Post system message to chat
                sendSystemMessage(subtask.parentTaskId, "📋 $userName submitted [${subtask.title}](#${subtask.id}) for review")

                // Notify creator
                val taskDoc = tasksCollection.document(subtask.parentTaskId).get().await()
                val creatorId = taskDoc.getString("creatorId")
                val taskTitle = taskDoc.getString("title") ?: "Task"
                
                if (creatorId != null && creatorId != auth.currentUser?.uid) {
                     notificationService.createNotification(
                        userId = creatorId,
                        title = "Subtask Awaiting Review",
                        body = "${subtask.title} in $taskTitle submitted for review",
                        type = NotificationType.SUBTASK_COMPLETED,
                        data = mapOf("taskId" to subtask.parentTaskId, "subtaskId" to subtask.id)
                    )
                }

                // Success - clean up
                localFile.delete()
                _pendingUploads.value = _pendingUploads.value.filter { it.subtaskId != subtask.id }
                
                fetchSubtasks(subtask.parentTaskId)
                _success.value = "✅ Evidence uploaded successfully. Awaiting creator review."
                _isLoading.value = false
                return
                
            } catch (e: Exception) {
                lastError = e
                Log.e("TaskViewModel", "Upload attempt ${attempt + 1} failed", e)
                
                if (attempt < maxRetries - 1) {
                    // Wait before retry with exponential backoff
                    val delayMs = (1000L * (attempt + 1) * 2)
                    Log.d("TaskViewModel", "Waiting ${delayMs}ms before retry...")
                    kotlinx.coroutines.delay(delayMs)
                }
            }
        }
        
        // All retries failed
        Log.e("TaskViewModel", "All upload attempts failed for ${subtask.title}", lastError)
        val updatedUpload = pendingUpload.copy(
            status = UploadStatus.FAILED,
            retryCount = maxRetries
        )
        _pendingUploads.value = _pendingUploads.value.map { 
            if (it.subtaskId == subtask.id) updatedUpload else it 
        }
        
        val errorMessage = when {
            lastError?.message?.contains("AppCheckProvider") == true -> 
                "❌ Upload failed: Network security error. Image saved locally - you can retry later."
            lastError?.message?.contains("network") == true -> 
                "❌ Upload failed: Network unavailable. Image saved locally - you can retry when online."
            else -> 
                "❌ Upload failed: ${lastError?.message ?: "Unknown error"}. Image saved locally - you can retry."
        }
        
        _error.value = errorMessage
        _isLoading.value = false
    }
    
    fun retryPendingUpload(pendingUpload: PendingUpload) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Update status to uploading
            _pendingUploads.value = _pendingUploads.value.map {
                if (it.subtaskId == pendingUpload.subtaskId) it.copy(status = UploadStatus.UPLOADING) else it
            }
            
            val localFile = java.io.File(pendingUpload.localCachePath)
            if (!localFile.exists()) {
                _error.value = "❌ Cached image not found. Please take a new photo."
                _pendingUploads.value = _pendingUploads.value.filter { it.subtaskId != pendingUpload.subtaskId }
                _isLoading.value = false
                return@launch
            }
            
            // Get fresh subtask data
            val subtaskDoc = subtasksCollection.document(pendingUpload.subtaskId).get().await()
            if (!subtaskDoc.exists()) {
                _error.value = "❌ Subtask no longer exists."
                localFile.delete()
                _pendingUploads.value = _pendingUploads.value.filter { it.subtaskId != pendingUpload.subtaskId }
                _isLoading.value = false
                return@launch
            }
            
            val subtask = Subtask.fromFirestore(subtaskDoc)
            uploadImageWithRetry(subtask, localFile, pendingUpload, maxRetries = 3)
        }
    }
    
    fun cancelPendingUpload(pendingUpload: PendingUpload) {
        val localFile = java.io.File(pendingUpload.localCachePath)
        localFile.delete()
        _pendingUploads.value = _pendingUploads.value.filter { it.subtaskId != pendingUpload.subtaskId }
        _success.value = "Upload cancelled."
    }

    private suspend fun checkParentTaskStatus(parentTaskId: String) {
        val snapshot = subtasksCollection
            .whereEqualTo("parentTaskId", parentTaskId)
            .get()
            .await()
        
        val allSubtasks = snapshot.documents.map { Subtask.fromFirestore(it) }
        val allCompleted = allSubtasks.all { it.status == SubtaskStatus.COMPLETED }
        
        val parentTaskRef = tasksCollection.document(parentTaskId)
        val parentTaskSnapshot = parentTaskRef.get().await()
        val parentTaskStatus = TaskStatus.valueOf((parentTaskSnapshot.getString("status") ?: "PENDING").uppercase())

        if (allCompleted && parentTaskStatus == TaskStatus.PENDING) {
            parentTaskRef.update("status", TaskStatus.IN_REVIEW.name.lowercase()).await()
        } else if (!allCompleted && (parentTaskStatus == TaskStatus.IN_REVIEW || parentTaskStatus == TaskStatus.COMPLETED)) {
            parentTaskRef.update("status", TaskStatus.PENDING.name.lowercase()).await()
        }
    }

    fun toggleSubtaskStatus(subtask: Subtask) {
        viewModelScope.launch {
            try {
                val newStatus = if (subtask.status == SubtaskStatus.PENDING) SubtaskStatus.COMPLETED else SubtaskStatus.PENDING
                
                subtasksCollection.document(subtask.id)
                    .update("status", newStatus.name.lowercase())
                    .await()
                
                // Send FCM update event
                val taskDoc = tasksCollection.document(subtask.parentTaskId).get().await()
                val task = Task.fromFirestore(taskDoc)
                val allUserIds = (task.assignedUserIds + task.creatorId + subtask.assignedUserIds).distinct()
                notificationService.sendUpdateEvent(
                    userIds = allUserIds,
                    eventType = "SUBTASK_STATUS_CHANGED",
                    data = mapOf(
                        "taskId" to subtask.parentTaskId,
                        "subtaskId" to subtask.id,
                        "newStatus" to newStatus.name
                    )
                )
                
                checkParentTaskStatus(subtask.parentTaskId)
                fetchSubtasks(subtask.parentTaskId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun placeBid(subtask: Subtask, amount: Double) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                Log.d("TaskViewModel", "placeBid: Starting for subtask ${subtask.id}, user $userId, amount $amount")
                
                val userDoc = db.collection("users").document(userId).get().await()
                val userName = userDoc.getString("name") ?: "User"
                
                val newBid = SubtaskBid(userId, amount, "pending")
                subtasksCollection.document(subtask.id)
                    .update("bids", com.google.firebase.firestore.FieldValue.arrayUnion(newBid.toMap()))
                    .await()

                Log.d("TaskViewModel", "placeBid: Bid saved to Firestore")

                // Get task info to notify all relevant users
                val taskDoc = tasksCollection.document(subtask.parentTaskId).get().await()
                val creatorId = taskDoc.getString("creatorId")
                val taskMembers = (taskDoc.get("memberIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val taskTitle = taskDoc.getString("title") ?: "Task"
                
                // Post system message to chat
                sendSystemMessage(subtask.parentTaskId, "💵 $userName placed a bid of $${String.format("%.2f", amount)} on: [${subtask.title}](#${subtask.id})")
                
                // Notify creator
                if (creatorId != null && creatorId != auth.currentUser?.uid) {
                    Log.d("TaskViewModel", "placeBid: Notifying creator $creatorId about new bid")
                    try {
                        notificationService.createNotificationAsync(
                            userId = creatorId,
                            title = "New Bid Received",
                            body = "A bid of $${String.format("%.2f", amount)} was placed on ${subtask.title}",
                            type = NotificationType.BUDGET_PROPOSAL,
                            data = mapOf("taskId" to subtask.parentTaskId, "subtaskId" to subtask.id)
                        )
                        Log.d("TaskViewModel", "placeBid: Creator notification sent")
                    } catch (e: Exception) {
                        Log.e("TaskViewModel", "placeBid: Error notifying creator", e)
                    }
                }

                // Notify other team members about the bid
                Log.d("TaskViewModel", "placeBid: Notifying ${taskMembers.size} team members")
                taskMembers.filter { it != userId && it != creatorId }.forEach { memberId ->
                    try {
                        notificationService.createNotificationAsync(
                            userId = memberId,
                            title = "New Bid on Subtask",
                            body = "${subtask.title}: New bid of $${String.format("%.2f", amount)} in $taskTitle",
                            type = NotificationType.BUDGET_PROPOSAL,
                            data = mapOf("taskId" to subtask.parentTaskId, "subtaskId" to subtask.id)
                        )
                        Log.d("TaskViewModel", "placeBid: Team member notification sent to $memberId")
                    } catch (e: Exception) {
                        Log.e("TaskViewModel", "placeBid: Error notifying team member $memberId", e)
                    }
                }

                // Send FCM event for reactive updates
                val task = Task.fromFirestore(taskDoc)
                val allUserIds = (task.assignedUserIds + task.creatorId + subtask.assignedUserIds).distinct()
                notificationService.sendUpdateEvent(
                    userIds = allUserIds,
                    eventType = "BID_PLACED",
                    data = mapOf(
                        "taskId" to subtask.parentTaskId,
                        "subtaskId" to subtask.id,
                        "amount" to amount.toString()
                    )
                )
                
                fetchSubtasks(subtask.parentTaskId)
                _success.value = "Bid placed successfully"
                Log.d("TaskViewModel", "placeBid: Completed successfully")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "placeBid: Error", e)
                _error.value = e.message
            }
        }
    }

    fun acceptBid(subtask: Subtask, bid: SubtaskBid) {
        viewModelScope.launch {
            try {
                Log.d("TaskViewModel", "acceptBid: Starting for subtask ${subtask.id}, bid user ${bid.userId}")
                
                val subtaskRef = subtasksCollection.document(subtask.id)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(subtaskRef)
                    val currentSubtask = Subtask.fromFirestore(snapshot)
                    
                    val updatedBids = currentSubtask.bids.map { 
                        if (it.userId == bid.userId && it.amount == bid.amount) {
                            it.copy(status = "accepted")
                        } else {
                            it.copy(status = "rejected") 
                        }
                    }
                    
                    val updatedAssignments = currentSubtask.assignments.toMutableMap()
                    updatedAssignments[bid.userId] = "accepted"
                    
                    val updatedAssignedIds = listOf(bid.userId)

                    Log.d("TaskViewModel", "acceptBid: Updating Firestore with assigned user ${bid.userId}")
                    transaction.update(subtaskRef, mapOf(
                        "bids" to updatedBids.map { it.toMap() },
                        "assignments" to updatedAssignments,
                        "assignedUserIds" to updatedAssignedIds,
                        "budget" to bid.amount  // Update budget to accepted bid amount
                    ))
                }.await()
                
                Log.d("TaskViewModel", "acceptBid: Transaction completed")
                
                // Get parent task info to notify all task members
                val parentTaskDoc = tasksCollection.document(subtask.parentTaskId).get().await()
                val creatorId = parentTaskDoc.getString("creatorId")
                val taskMembers = (parentTaskDoc.get("memberIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val subtaskTitle = subtask.title
                
                // Get user names
                val currentUserDoc = db.collection("users").document(auth.currentUser?.uid ?: "").get().await()
                val currentUserName = currentUserDoc.getString("name") ?: "User"
                val winnerDoc = db.collection("users").document(bid.userId).get().await()
                val winnerName = winnerDoc.getString("name") ?: "User"
                
                // Post system messages to chat
                sendSystemMessage(subtask.parentTaskId, "✅ $currentUserName accepted $winnerName's bid of $${String.format("%.2f", bid.amount)} for: [$subtaskTitle](#${subtask.id})")
                sendSystemMessage(subtask.parentTaskId, "👤 $winnerName was assigned to: [$subtaskTitle](#${subtask.id})")
                
                // Update parent task dueDate to the farthest subtask dueDate
                updateParentTaskDueDate(subtask.parentTaskId)
                calculateTotalBudget(subtask.parentTaskId)
                
                // Notify the winning bidder
                Log.d("TaskViewModel", "acceptBid: Sending acceptance notification to winning bidder ${bid.userId}")
                try {
                    notificationService.createNotificationAsync(
                        userId = bid.userId,
                        title = "Bid Accepted",
                        body = "Your bid of $${String.format("%.2f", bid.amount)} was accepted for $subtaskTitle",
                        type = NotificationType.BUDGET_PROPOSAL,
                        data = mapOf("taskId" to subtask.parentTaskId, "subtaskId" to subtask.id)
                    )
                    Log.d("TaskViewModel", "acceptBid: Acceptance notification sent to winning bidder")
                } catch (e: Exception) {
                    Log.e("TaskViewModel", "acceptBid: Notification error for winning bidder", e)
                }
                
                // Notify other rejected bidders
                subtask.bids.filter { it.userId != bid.userId }.forEach { rejectedBid ->
                    Log.d("TaskViewModel", "acceptBid: Sending rejection notification to ${rejectedBid.userId}")
                    try {
                        notificationService.createNotificationAsync(
                            userId = rejectedBid.userId,
                            title = "Bid Not Selected",
                            body = "Your bid of $${String.format("%.2f", rejectedBid.amount)} was not selected for $subtaskTitle",
                            type = NotificationType.BUDGET_PROPOSAL,
                            data = mapOf("taskId" to subtask.parentTaskId, "subtaskId" to subtask.id)
                        )
                    } catch (e: Exception) {
                        Log.e("TaskViewModel", "acceptBid: Notification error for rejected bidder ${rejectedBid.userId}", e)
                    }
                }
                
                // Notify task creator and other members about the accepted bid
                val creatorNotificationList = mutableListOf<String>()
                if (creatorId != null && creatorId != auth.currentUser?.uid) {
                    creatorNotificationList.add(creatorId)
                }
                
                // Add other task members who are not the winning bidder
                taskMembers.filter { it != bid.userId && it != auth.currentUser?.uid }.forEach { memberId ->
                    if (!creatorNotificationList.contains(memberId)) {
                        creatorNotificationList.add(memberId)
                    }
                }
                
                Log.d("TaskViewModel", "acceptBid: Notifying ${creatorNotificationList.size} other users about bid acceptance")
                creatorNotificationList.forEach { userId ->
                    try {
                        notificationService.createNotificationAsync(
                            userId = userId,
                            title = "Bid Accepted for Subtask",
                            body = "$subtaskTitle: ${subtask.assignedUserIds.firstOrNull()?.let { "Assigned to team member" } ?: "Assigned"}. Budget: $${String.format("%.2f", bid.amount)}",
                            type = NotificationType.BUDGET_PROPOSAL,
                            data = mapOf("taskId" to subtask.parentTaskId, "subtaskId" to subtask.id)
                        )
                        Log.d("TaskViewModel", "acceptBid: Notification sent to $userId")
                    } catch (e: Exception) {
                        Log.e("TaskViewModel", "acceptBid: Notification error for user $userId", e)
                    }
                }
                
                // Send FCM event for reactive updates
                val taskDoc = tasksCollection.document(subtask.parentTaskId).get().await()
                val task = Task.fromFirestore(taskDoc)
                val allUserIds = (task.assignedUserIds + task.creatorId + subtask.assignedUserIds).distinct()
                notificationService.sendUpdateEvent(
                    userIds = allUserIds,
                    eventType = "BID_ACCEPTED",
                    data = mapOf(
                        "taskId" to subtask.parentTaskId,
                        "subtaskId" to subtask.id,
                        "userId" to bid.userId
                    )
                )
                
                Log.d("TaskViewModel", "acceptBid: Calling fetchSubtasks for parent task ${subtask.parentTaskId}")
                fetchSubtasks(subtask.parentTaskId)
                _success.value = "Bid accepted successfully"
                Log.d("TaskViewModel", "acceptBid: Completed successfully")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "acceptBid: Error", e)
                _error.value = e.message
            }
        }
    }

    fun approveTask(taskId: String) {
        viewModelScope.launch {
            try {
                tasksCollection.document(taskId)
                    .update("status", TaskStatus.COMPLETED.name.lowercase())
                    .await()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun rejectTask(taskId: String) {
        viewModelScope.launch {
            try {
                tasksCollection.document(taskId)
                    .update("status", TaskStatus.PENDING.name.lowercase())
                    .await()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun requestPostponement(subtask: Subtask, newDate: java.util.Date, reason: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val request = PostponementRequest(userId, newDate, reason, "pending")
                subtasksCollection.document(subtask.id)
                    .update("postponementRequests", com.google.firebase.firestore.FieldValue.arrayUnion(request.toMap()))
                    .await()

                // Notify creator
                val taskDoc = tasksCollection.document(subtask.parentTaskId).get().await()
                val creatorId = taskDoc.getString("creatorId")
                val taskTitle = taskDoc.getString("title") ?: "Task"
                
                if (creatorId != null && creatorId != auth.currentUser?.uid) {
                     notificationService.createNotification(
                        userId = creatorId,
                        title = "Postponement Request",
                        body = "Postponement requested for ${subtask.title}",
                        type = NotificationType.DEADLINE_EXTENSION_REQUEST,
                        data = mapOf("taskId" to subtask.parentTaskId, "subtaskId" to subtask.id)
                    )
                    
                    // Send FCM for real-time update
                    notificationService.sendUpdateEvent(
                        userIds = listOf(creatorId),
                        eventType = "POSTPONEMENT_REQUESTED",
                        data = mapOf(
                            "taskId" to subtask.parentTaskId,
                            "subtaskId" to subtask.id,
                            "requestedDate" to newDate.time.toString()
                        )
                    )
                }

                fetchSubtasks(subtask.parentTaskId)
                _success.value = "Postponement request sent"
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun respondToPostponement(subtask: Subtask, request: PostponementRequest, accept: Boolean) {
        viewModelScope.launch {
            try {
                val subtaskRef = subtasksCollection.document(subtask.id)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(subtaskRef)
                    val currentSubtask = Subtask.fromFirestore(snapshot)
                    
                    val updatedRequests = currentSubtask.postponementRequests.map { 
                        if (it.userId == request.userId && it.requestedDate == request.requestedDate && it.reason == request.reason) {
                            it.copy(status = if (accept) "accepted" else "rejected")
                        } else {
                            it
                        }
                    }
                    
                    val updates = mutableMapOf<String, Any>(
                        "postponementRequests" to updatedRequests.map { it.toMap() }
                    )
                    
                    if (accept) {
                        updates["dueDate"] = com.google.firebase.Timestamp(request.requestedDate)
                    }

                    transaction.update(subtaskRef, updates)
                }.await()
                
                // Update parent task dueDate to the farthest subtask dueDate
                updateParentTaskDueDate(subtask.parentTaskId)
                
                // Notify requester about decision
                notificationService.createNotificationAsync(
                    userId = request.userId,
                    title = if (accept) "Postponement Accepted" else "Postponement Rejected",
                    body = "${subtask.title}: Your postponement request was ${if (accept) "accepted" else "rejected"}",
                    type = if (accept) NotificationType.DEADLINE_EXTENSION_APPROVAL else NotificationType.DEADLINE_EXTENSION_REJECTION,
                    data = mapOf("taskId" to subtask.parentTaskId, "subtaskId" to subtask.id)
                )
                
                // Send FCM for real-time update to requester and all task members
                val taskDoc = tasksCollection.document(subtask.parentTaskId).get().await()
                val task = Task.fromFirestore(taskDoc)
                val allUserIds = (task.assignedUserIds + task.creatorId + subtask.assignedUserIds).distinct()
                
                notificationService.sendUpdateEvent(
                    userIds = allUserIds,
                    eventType = if (accept) "POSTPONEMENT_ACCEPTED" else "POSTPONEMENT_REJECTED",
                    data = mapOf(
                        "taskId" to subtask.parentTaskId,
                        "subtaskId" to subtask.id,
                        "requesterId" to request.userId
                    )
                )
                
                fetchSubtasks(subtask.parentTaskId)
                _success.value = "Postponement ${if (accept) "accepted" else "rejected"} successfully"
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private suspend fun updateParentTaskDueDate(parentTaskId: String) {
        try {
            val snapshot = subtasksCollection
                .whereEqualTo("parentTaskId", parentTaskId)
                .get()
                .await()
            
            val allSubtasks = snapshot.documents.map { Subtask.fromFirestore(it) }
            val farthestDueDate = allSubtasks
                .mapNotNull { it.dueDate }
                .maxOrNull()
            
            if (farthestDueDate != null) {
                tasksCollection.document(parentTaskId)
                    .update("dueDate", com.google.firebase.Timestamp(farthestDueDate))
                    .await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("TaskViewModel", "updateParentTaskDueDate: Offline or error, operation will sync later", e)
            // Firestore will automatically retry when connection is restored due to offline persistence
        }
    }

    fun setPriorityFilter(priority: TaskPriority?) {
        _filterPriority.value = priority
        applyFilters()
    }

    fun setDateRangeFilter(start: Long?, end: Long?) {
        if (start != null && end != null) {
            _filterDateRange.value = start to end
        } else {
            _filterDateRange.value = null
        }
        applyFilters()
    }

    private fun applyFilters() {
        var filtered = _allTasks
        
        _filterPriority.value?.let { p ->
            filtered = filtered.filter { it.priority == p }
        }
        
        _filterDateRange.value?.let { (start, end) ->
            filtered = filtered.filter { task ->
                task.dueDate.time in start..end
            }
        }
        
        _tasks.value = sortTasks(filtered)
    }

    fun fetchUserTasks() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val snapshot = tasksCollection
                    .whereArrayContains("assignedUserIds", userId)
                    .get()
                    .await()

                val fetchedTasks = snapshot.documents.mapNotNull { 
                    try {
                        Task.fromFirestore(it)
                    } catch (e: Exception) {
                        android.util.Log.e("TaskViewModel", "Error parsing task ${it.id}", e)
                        null
                    }
                }
                
                _allTasks = fetchedTasks
                applyFilters()
                
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchGroupTasks(groupId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = tasksCollection
                    .whereEqualTo("groupId", groupId)
                    .get()
                    .await()

                val fetchedTasks = snapshot.documents.map { Task.fromFirestore(it) }
                _allTasks = fetchedTasks
                applyFilters()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun sortTasks(tasks: List<Task>): List<Task> {
        return tasks.sortedWith(
            compareByDescending<Task> { it.priority } 
                .thenBy { it.dueDate } 
        )
    }

    fun createTask(
        title: String,
        description: String,
        dueDate: java.util.Date,
        priority: TaskPriority,
        groupId: String? = null,
        assignedUserIds: List<String> = emptyList(),
        onSuccess: () -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d("TaskViewModel", "createTask called with title: $title, current tasks: ${_tasks.value.size}")
                
                // Check if user can create more tasks
                val company = CompanyPlanProvider.currentCompany.value
                val plan = CompanyPlanProvider.currentPlan.value
                
                // Create effective company for users without a company
                val effectiveCompany = company ?: Company(
                    id = "temp",
                    planId = "free",
                    planTier = PlanTier.FREE,
                    activeTasksCount = _tasks.value.size,
                    ownerId = userId
                )
                
                android.util.Log.d("TaskViewModel", "Limit check - company: ${if (company != null) "exists" else "null"}, plan: ${plan.tier}, current tasks: ${effectiveCompany.activeTasksCount}")
                
                val (canCreate, reason) = FeatureFlags.canCreateTask(effectiveCompany, plan)
                android.util.Log.d("TaskViewModel", "Limit check result - canCreate: $canCreate, reason: $reason")
                
                if (!canCreate) {
                    android.util.Log.e("TaskViewModel", "TASK LIMIT REACHED: $reason")
                    _error.value = reason ?: "Task limit reached - upgrade to create more tasks"
                    _isLoading.value = false
                    return@launch
                }
                
                val userIds = (assignedUserIds + userId).distinct()
                val newTask = hashMapOf(
                    "title" to title,
                    "description" to description,
                    "dueDate" to com.google.firebase.Timestamp(dueDate),
                    "priority" to priority.name.lowercase(),
                    "status" to "pending",
                    "creatorId" to userId,
                    "assignedUserIds" to userIds,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "groupId" to groupId
                )

                val docRef = tasksCollection.add(newTask).await()

                // Update company activeTasksCount if user has a company
                company?.let { comp ->
                    db.collection("companies")
                        .document(comp.id)
                        .update("activeTasksCount", com.google.firebase.firestore.FieldValue.increment(1))
                        .await()
                }

                // Get current user name
                val currentUserDoc = db.collection("users").document(userId).get().await()
                val currentUserName = currentUserDoc.getString("name") ?: "User"
                
                // Add system message for task creation
                sendSystemMessage(docRef.id, "🎯 $currentUserName created this task")
                
                // Add system messages for initially assigned members
                val recipients = userIds.filter { it != userId }
                for (recipientId in recipients) {
                    try {
                        val userDoc = db.collection("users").document(recipientId).get().await()
                        val userName = userDoc.getString("name") ?: "User"
                        sendSystemMessage(docRef.id, "➕ $currentUserName added $userName to the task")
                    } catch (e: Exception) {
                        Log.e("TaskViewModel", "createTask: Error getting user name", e)
                    }
                }
                
                if (recipients.isNotEmpty()) {
                    notificationService.notifyTaskMembers(
                        memberIds = recipients,
                        title = "New Task Assigned",
                        body = "You have been assigned to the task: $title",
                        type = NotificationType.TASK_ASSIGNED,
                        data = mapOf("taskId" to docRef.id)
                    )
                }

                if (groupId != null) {
                    fetchGroupTasks(groupId)
                } else {
                    fetchUserTasks()
                }
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTaskAssignments(taskId: String, userIds: List<String>, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get current task to compare assignments
                val taskDoc = tasksCollection.document(taskId).get().await()
                val currentUserIds = (taskDoc.get("assignedUserIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                
                // Update assignments
                tasksCollection.document(taskId).update("assignedUserIds", userIds).await()
                
                // Get current user name
                val currentUserDoc = db.collection("users").document(auth.currentUser?.uid ?: "").get().await()
                val currentUserName = currentUserDoc.getString("name") ?: "User"
                
                // Detect added members
                val addedUserIds = userIds.filter { it !in currentUserIds }
                for (userId in addedUserIds) {
                    try {
                        val userDoc = db.collection("users").document(userId).get().await()
                        val userName = userDoc.getString("name") ?: "User"
                        sendSystemMessage(taskId, "➕ $currentUserName added $userName to the task")
                    } catch (e: Exception) {
                        Log.e("TaskViewModel", "updateTaskAssignments: Error getting user name", e)
                    }
                }
                
                // Detect removed members
                val removedUserIds = currentUserIds.filter { it !in userIds }
                for (userId in removedUserIds) {
                    try {
                        val userDoc = db.collection("users").document(userId).get().await()
                        val userName = userDoc.getString("name") ?: "User"
                        sendSystemMessage(taskId, "➖ $currentUserName removed $userName from the task")
                    } catch (e: Exception) {
                        Log.e("TaskViewModel", "updateTaskAssignments: Error getting user name", e)
                    }
                }
                
                onComplete()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    

    fun getTask(taskId: String): StateFlow<Task?> { 
        val taskFlow = MutableStateFlow<Task?>(null)
        val registration = tasksCollection.document(taskId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                taskFlow.value = Task.fromFirestore(snapshot)
            }
        }
        
        return taskFlow.asStateFlow()
    }

    fun getTaskFlow(taskId: String): kotlinx.coroutines.flow.Flow<Task?> = kotlinx.coroutines.flow.callbackFlow {
        val registration = tasksCollection.document(taskId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(Task.fromFirestore(snapshot))
            }
        }
        awaitClose { registration.remove() }
    }
    
    fun sendBudgetStatusToChat(taskId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                
                // Get all earnings for this task
                val allUsersSnapshot = db.collection("users").get().await()
                var totalPending = 0.0
                var totalConfirmed = 0.0
                val pendingUsers = mutableListOf<String>()
                val confirmedUsers = mutableListOf<String>()
                
                for (userDoc in allUsersSnapshot.documents) {
                    val earningsSnapshot = userDoc.reference
                        .collection("earnings")
                        .whereEqualTo("taskId", taskId)
                        .whereEqualTo("creatorId", currentUserId)
                        .get()
                        .await()
                    
                    for (earningDoc in earningsSnapshot.documents) {
                        val amount = earningDoc.getDouble("amount") ?: 0.0
                        val status = earningDoc.getString("status") ?: "pending"
                        val userName = userDoc.getString("name") ?: "User"
                        
                        if (status == "confirmed") {
                            totalConfirmed += amount
                            confirmedUsers.add("$userName ($${String.format("%.2f", amount)})")
                        } else {
                            totalPending += amount
                            pendingUsers.add("$userName ($${String.format("%.2f", amount)})")
                        }
                    }
                }
                
                // Build status message
                val messageBuilder = StringBuilder("📊 Budget Status Summary:\n\n")
                messageBuilder.append("💰 Total Paid (Confirmed): $${String.format("%.2f", totalConfirmed)}\n")
                if (confirmedUsers.isNotEmpty()) {
                    messageBuilder.append("  ✅ ${confirmedUsers.joinToString(", ")}\n")
                }
                messageBuilder.append("\n⏳ Total Pending: $${String.format("%.2f", totalPending)}\n")
                if (pendingUsers.isNotEmpty()) {
                    messageBuilder.append("  ⏱️ ${pendingUsers.joinToString(", ")}\n")
                }
                
                sendSystemMessage(taskId, messageBuilder.toString())
                
            } catch (e: Exception) {
                _error.value = "Failed to send budget status: ${e.message}"
            }
        }
    }

    fun fetchMessages(taskId: String) {
        tasksCollection.document(taskId).collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = e.message
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val msgs = snapshot.documents.map { TaskMessage.fromFirestore(it) }
                    _messages.value = msgs
                }
            }
    }

    fun sendMessage(taskId: String, text: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                Log.d("TaskViewModel", "sendMessage: Starting for task $taskId")
                
                val userDoc = db.collection("users").document(userId).get().await()
                val userName = userDoc.getString("name") ?: auth.currentUser?.displayName ?: "Unknown"

                val message = hashMapOf(
                    "text" to text,
                    "authorId" to userId,
                    "authorName" to userName,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                
                tasksCollection.document(taskId).collection("comments").add(message).await()
                Log.d("TaskViewModel", "sendMessage: Message saved to Firestore")
                
                // Get task info to notify all members
                val taskDoc = tasksCollection.document(taskId).get().await()
                val taskTitle = taskDoc.getString("title") ?: "Task"
                val creatorId = taskDoc.getString("creatorId")
                val taskMembers = (taskDoc.get("memberIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                
                // Prepare notification body (truncate long messages)
                val messagePreview = if (text.length > 50) "${text.take(50)}..." else text
                
                // Notify task creator if not the sender
                if (creatorId != null && creatorId != userId) {
                    Log.d("TaskViewModel", "sendMessage: Notifying creator $creatorId")
                    try {
                        notificationService.createNotificationAsync(
                            userId = creatorId,
                            title = "New Message in $taskTitle",
                            body = "$userName: $messagePreview",
                            type = NotificationType.TASK_COMMENT,
                            data = mapOf("taskId" to taskId)
                        )
                    } catch (e: Exception) {
                        Log.e("TaskViewModel", "sendMessage: Error notifying creator", e)
                    }
                }
                
                // Notify all team members except the sender and creator (already notified)
                Log.d("TaskViewModel", "sendMessage: Notifying ${taskMembers.size} team members")
                taskMembers.filter { it != userId && it != creatorId }.forEach { memberId ->
                    try {
                        notificationService.createNotificationAsync(
                            userId = memberId,
                            title = "New Message in $taskTitle",
                            body = "$userName: $messagePreview",
                            type = NotificationType.TASK_COMMENT,
                            data = mapOf("taskId" to taskId)
                        )
                        Log.d("TaskViewModel", "sendMessage: Notification sent to $memberId")
                    } catch (e: Exception) {
                        Log.e("TaskViewModel", "sendMessage: Error notifying member $memberId", e)
                    }
                }
                
                _success.value = "Message sent successfully"
                Log.d("TaskViewModel", "sendMessage: Completed successfully")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "sendMessage: Error", e)
                _error.value = e.message
            }
        }
    }

    private suspend fun sendSystemMessage(taskId: String, message: String) {
        try {
            val systemMessage = hashMapOf(
                "text" to message,
                "authorId" to "system",
                "authorName" to "System",
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            tasksCollection.document(taskId).collection("comments").add(systemMessage).await()
            Log.d("TaskViewModel", "sendSystemMessage: System message posted to chat")
        } catch (e: Exception) {
            Log.e("TaskViewModel", "sendSystemMessage: Error", e)
        }
    }

    fun requestTaskPostponement(taskId: String, newDate: java.util.Date, reason: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(userId).get().await()
                val userName = userDoc.getString("name") ?: "User"
                val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                
                val request = PostponementRequest(userId, newDate, reason, "pending")
                tasksCollection.document(taskId)
                    .update("postponementRequests", com.google.firebase.firestore.FieldValue.arrayUnion(request.toMap()))
                    .await()
                
                // Post system message to chat
                sendSystemMessage(taskId, "⏱️ $userName requested postponement to ${dateFormat.format(newDate)}. Reason: $reason")

                val taskDoc = tasksCollection.document(taskId).get().await()
                val creatorId = taskDoc.getString("creatorId")
                val taskTitle = taskDoc.getString("title") ?: "Task"
                
                if (creatorId != null && creatorId != auth.currentUser?.uid) {
                     notificationService.createNotification(
                        userId = creatorId,
                        title = "Task Postponement Request",
                        body = "Postponement requested for $taskTitle",
                        type = NotificationType.DEADLINE_EXTENSION_REQUEST,
                        data = mapOf("taskId" to taskId)
                    )
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun respondToTaskPostponement(taskId: String, request: PostponementRequest, accept: Boolean) {
        viewModelScope.launch {
            try {
                val taskRef = tasksCollection.document(taskId)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(taskRef)
                    val currentTask = Task.fromFirestore(snapshot)
                    
                    val updatedRequests = currentTask.postponementRequests.map { 
                        if (it.userId == request.userId && it.requestedDate == request.requestedDate && it.reason == request.reason) {
                            it.copy(status = if (accept) "accepted" else "rejected")
                        } else {
                            it
                        }
                    }
                    
                    val updates = mutableMapOf<String, Any>(
                        "postponementRequests" to updatedRequests.map { it.toMap() }
                    )
                    
                    if (accept) {
                        updates["dueDate"] = com.google.firebase.Timestamp(request.requestedDate)
                    }

                    transaction.update(taskRef, updates)
                }.await()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearSuccess() {
        _success.value = null
    }

    fun updateSubtaskBudget(subtaskId: String, budget: Double, parentTaskId: String) {
        viewModelScope.launch {
            try {
                Log.d("TaskViewModel", "updateSubtaskBudget: Starting for subtask $subtaskId")
                val userDoc = db.collection("users").document(auth.currentUser?.uid ?: "").get().await()
                val userName = userDoc.getString("name") ?: "User"
                
                // Get subtask details for notification
                val subtaskSnap = subtasksCollection.document(subtaskId).get().await()
                val subtask = Subtask.fromFirestore(subtaskSnap)
                
                subtasksCollection.document(subtaskId).update("budget", budget).await()
                
                // Post system message to chat
                sendSystemMessage(parentTaskId, "💰 $userName updated budget to $${String.format("%.2f", budget)} for: [${subtask.title}](#${subtask.id})")
                
                calculateTotalBudget(parentTaskId)
                updateParentTaskDueDate(parentTaskId)
                
                Log.d("TaskViewModel", "updateSubtaskBudget: Notifying ${subtask.assignedUserIds.size} assigned users")
                // Notify all assigned users about budget change
                subtask.assignedUserIds.forEach { userId ->
                    if (userId != auth.currentUser?.uid) {
                        try {
                            notificationService.createNotificationAsync(
                                userId = userId,
                                title = "Subtask Budget Updated",
                                body = "${subtask.title} budget changed to $${String.format("%.2f", budget)}",
                                type = NotificationType.BUDGET_PROPOSAL,
                                data = mapOf("taskId" to parentTaskId, "subtaskId" to subtaskId)
                            )
                        } catch (e: Exception) {
                            Log.e("TaskViewModel", "updateSubtaskBudget: Notification error for user $userId", e)
                        }
                    }
                }
                
                // Send FCM event for reactive updates
                val taskDoc = tasksCollection.document(parentTaskId).get().await()
                val task = Task.fromFirestore(taskDoc)
                val allUserIds = (task.assignedUserIds + task.creatorId + subtask.assignedUserIds).distinct()
                notificationService.sendUpdateEvent(
                    userIds = allUserIds,
                    eventType = "SUBTASK_UPDATED",
                    data = mapOf(
                        "taskId" to parentTaskId,
                        "subtaskId" to subtaskId
                    )
                )
                
                fetchSubtasks(parentTaskId)
                _success.value = "Budget updated successfully"
                Log.d("TaskViewModel", "updateSubtaskBudget: Completed successfully")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "updateSubtaskBudget: Error", e)
                _error.value = e.message
            }
        }
    }

    fun updateSubtaskDueDate(subtaskId: String, dueDate: Date, parentTaskId: String) {
        viewModelScope.launch {
            try {
                Log.d("TaskViewModel", "updateSubtaskDueDate: Starting for subtask $subtaskId")
                val userDoc = db.collection("users").document(auth.currentUser?.uid ?: "").get().await()
                val userName = userDoc.getString("name") ?: "User"
                
                // Get subtask details for notification
                val subtaskSnap = subtasksCollection.document(subtaskId).get().await()
                val subtask = Subtask.fromFirestore(subtaskSnap)
                
                val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                
                subtasksCollection.document(subtaskId).update("dueDate", com.google.firebase.Timestamp(dueDate)).await()
                
                // Post system message to chat
                sendSystemMessage(parentTaskId, "📅 $userName changed due date to ${dateFormat.format(dueDate)} for: [${subtask.title}](#${subtask.id})")
                
                updateParentTaskDueDate(parentTaskId)
                
                Log.d("TaskViewModel", "updateSubtaskDueDate: Notifying ${subtask.assignedUserIds.size} assigned users")
                // Notify all assigned users about due date change
                subtask.assignedUserIds.forEach { userId ->
                    if (userId != auth.currentUser?.uid) {
                        try {
                            notificationService.createNotificationAsync(
                                userId = userId,
                                title = "Subtask Due Date Changed",
                                body = "${subtask.title} new due date: ${dateFormat.format(dueDate)}",
                                type = NotificationType.BUDGET_PROPOSAL,
                                data = mapOf("taskId" to parentTaskId, "subtaskId" to subtaskId)
                            )
                        } catch (e: Exception) {
                            Log.e("TaskViewModel", "updateSubtaskDueDate: Notification error for user $userId", e)
                        }
                    }
                }
                
                fetchSubtasks(parentTaskId)
                _success.value = "Due date updated successfully"
                Log.d("TaskViewModel", "updateSubtaskDueDate: Completed successfully")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "updateSubtaskDueDate: Error", e)
                _error.value = e.message
            }
        }
    }

    fun approveCompletion(subtask: Subtask, parentTaskId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userDoc = db.collection("users").document(auth.currentUser?.uid ?: "").get().await()
                val userName = userDoc.getString("name") ?: "User"
                
                // Update subtask status to COMPLETED
                subtasksCollection.document(subtask.id).update(
                    "status", SubtaskStatus.COMPLETED.name.lowercase()
                ).await()
                
                // Post system message to chat
                sendSystemMessage(parentTaskId, "✅ $userName approved completion of: [${subtask.title}](#${subtask.id})")

                // Distribute budget to all assigned users who completed it
                subtask.budget?.let { budget ->
                    if (budget > 0 && subtask.assignedUserIds.isNotEmpty()) {
                        val budgetPerUser = budget / subtask.assignedUserIds.size
                        
                        // Get names of assigned users
                        val userNames = mutableListOf<String>()
                        
                        // Create completion record for each assigned user
                        subtask.assignedUserIds.forEach { userId ->
                            try {
                                // Get user name
                                val assignedUserDoc = db.collection("users").document(userId).get().await()
                                val assignedUserName = assignedUserDoc.getString("name") ?: "User"
                                userNames.add(assignedUserName)
                                
                                val completionRecord = mapOf(
                                    "userId" to userId,
                                    "subtaskId" to subtask.id,
                                    "taskId" to parentTaskId,
                                    "subtaskTitle" to subtask.title,
                                    "amount" to budgetPerUser,
                                    "timestamp" to com.google.firebase.Timestamp.now(),
                                    "status" to "pending", // pending, confirmed
                                    "creatorId" to auth.currentUser?.uid
                                )
                                // Store in user's earnings or budget allocation collection
                                db.collection("users").document(userId)
                                    .collection("earnings")
                                    .add(completionRecord)
                                    .await()
                            } catch (e: Exception) {
                                _error.value = "Failed to allocate budget to user: ${e.message}"
                            }
                        }
                        
                        // Send budget distribution message to chat
                        val usersText = if (userNames.size == 1) {
                            userNames[0]
                        } else {
                            userNames.dropLast(1).joinToString(", ") + " and ${userNames.last()}"
                        }
                        sendSystemMessage(
                            parentTaskId, 
                            "💰 Budget distributed: $${String.format("%.2f", budgetPerUser)} per person to $usersText (Status: Pending confirmation)"
                        )
                    }
                }

                // Notify assigned users about completion approval
                subtask.assignedUserIds.forEach { userId ->
                    if (userId != auth.currentUser?.uid) {
                        notificationService.createNotification(
                            userId = userId,
                            title = "Completion Approved",
                            body = "${subtask.title} completion was approved. Budget allocated.",
                            type = NotificationType.SUBTASK_COMPLETED,
                            data = mapOf("taskId" to parentTaskId, "subtaskId" to subtask.id)
                        )
                    }
                }
                
                // Send FCM update event to all task members for UI refresh
                val taskDoc = tasksCollection.document(parentTaskId).get().await()
                val task = Task.fromFirestore(taskDoc)
                val allUserIds = (task.assignedUserIds + task.creatorId + subtask.assignedUserIds).distinct()
                notificationService.sendUpdateEvent(
                    userIds = allUserIds,
                    eventType = "BUDGET_STATUS_CHANGED",
                    data = mapOf(
                        "taskId" to parentTaskId,
                        "subtaskId" to subtask.id
                    )
                )

                checkParentTaskStatus(parentTaskId)
                fetchSubtasks(parentTaskId)
                _success.value = "Completion approved successfully"
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun rejectCompletion(subtask: Subtask, parentTaskId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userDoc = db.collection("users").document(auth.currentUser?.uid ?: "").get().await()
                val userName = userDoc.getString("name") ?: "User"
                
                // Revert to PENDING status
                subtasksCollection.document(subtask.id).update(
                    mapOf(
                        "status" to SubtaskStatus.PENDING.name.lowercase(),
                        "confirmationImageUrl" to com.google.firebase.firestore.FieldValue.delete()
                    )
                ).await()
                
                // Post system message to chat
                sendSystemMessage(parentTaskId, "❌ $userName rejected completion of: [${subtask.title}](#${subtask.id})")

                // Notify assigned users about rejection
                subtask.assignedUserIds.forEach { userId ->
                    if (userId != auth.currentUser?.uid) {
                        notificationService.createNotification(
                            userId = userId,
                            title = "Completion Rejected",
                            body = "${subtask.title} completion was rejected. Please resubmit.",
                            type = NotificationType.SUBTASK_COMPLETED,
                            data = mapOf("taskId" to parentTaskId, "subtaskId" to subtask.id)
                        )
                    }
                }

                // Send FCM event for reactive updates
                val taskDoc = tasksCollection.document(parentTaskId).get().await()
                val task = Task.fromFirestore(taskDoc)
                val allUserIds = (task.assignedUserIds + task.creatorId + subtask.assignedUserIds).distinct()
                notificationService.sendUpdateEvent(
                    userIds = allUserIds,
                    eventType = "SUBTASK_COMPLETION_REJECTED",
                    data = mapOf(
                        "taskId" to parentTaskId,
                        "subtaskId" to subtask.id
                    )
                )

                fetchSubtasks(parentTaskId)
                _success.value = "Completion rejected. Subtask reset to pending."
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get all subtasks to notify assigned users
                val subtasksSnapshot = subtasksCollection
                    .whereEqualTo("parentTaskId", taskId)
                    .get()
                    .await()
                
                val subtasks = subtasksSnapshot.documents.map { Subtask.fromFirestore(it) }
                val assignedUserIds = subtasks.flatMap { it.assignedUserIds }.distinct()

                // Delete all chat threads associated with this task
                val taskChatsSnapshot = db.collection("chatThreads")
                    .whereEqualTo("taskId", taskId)
                    .get()
                    .await()
                
                taskChatsSnapshot.documents.forEach { chatDoc ->
                    // Delete all messages in this chat thread
                    val messagesSnapshot = db.collection("messages")
                        .whereEqualTo("chatThreadId", chatDoc.id)
                        .get()
                        .await()
                    
                    messagesSnapshot.documents.forEach { msgDoc ->
                        msgDoc.reference.delete().await()
                    }
                    
                    // Delete the chat thread
                    chatDoc.reference.delete().await()
                }

                // Delete all subtasks and their associated chats
                subtasksSnapshot.documents.forEach { doc ->
                    val subtaskId = doc.id
                    
                    // Delete chat threads for this subtask
                    val subtaskChatsSnapshot = db.collection("chatThreads")
                        .whereEqualTo("subtaskId", subtaskId)
                        .get()
                        .await()
                    
                    subtaskChatsSnapshot.documents.forEach { chatDoc ->
                        // Delete messages
                        val messagesSnapshot = db.collection("messages")
                            .whereEqualTo("chatThreadId", chatDoc.id)
                            .get()
                            .await()
                        
                        messagesSnapshot.documents.forEach { msgDoc ->
                            msgDoc.reference.delete().await()
                        }
                        
                        // Delete chat thread
                        chatDoc.reference.delete().await()
                    }
                    
                    // Delete the subtask
                    doc.reference.delete().await()
                }

                // Delete the task
                tasksCollection.document(taskId).delete().await()

                // Notify all assigned users
                assignedUserIds.forEach { userId ->
                    if (userId != auth.currentUser?.uid) {
                        notificationService.createNotification(
                            userId = userId,
                            title = "Task Deleted",
                            body = "A task you were assigned to has been deleted",
                            type = NotificationType.TASK_DELETED,
                            data = mapOf("taskId" to taskId)
                        )
                    }
                }

                // Send FCM event for reactive updates
                notificationService.sendUpdateEvent(
                    userIds = assignedUserIds,
                    eventType = "TASK_DELETED",
                    data = mapOf("taskId" to taskId)
                )

                _tasks.value = _tasks.value.filter { it.id != taskId }
                _success.value = "Task deleted successfully"
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteSubtask(subtaskId: String, parentTaskId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get subtask details
                val subtaskSnap = subtasksCollection.document(subtaskId).get().await()
                val subtask = Subtask.fromFirestore(subtaskSnap)

                // Delete all chat threads associated with this subtask
                val subtaskChatsSnapshot = db.collection("chatThreads")
                    .whereEqualTo("subtaskId", subtaskId)
                    .get()
                    .await()
                
                subtaskChatsSnapshot.documents.forEach { chatDoc ->
                    // Delete all messages in this chat thread
                    val messagesSnapshot = db.collection("messages")
                        .whereEqualTo("chatThreadId", chatDoc.id)
                        .get()
                        .await()
                    
                    messagesSnapshot.documents.forEach { msgDoc ->
                        msgDoc.reference.delete().await()
                    }
                    
                    // Delete the chat thread
                    chatDoc.reference.delete().await()
                }

                // Delete the subtask
                subtasksCollection.document(subtaskId).delete().await()

                // Notify all assigned users
                subtask.assignedUserIds.forEach { userId ->
                    if (userId != auth.currentUser?.uid) {
                        notificationService.createNotification(
                            userId = userId,
                            title = "Subtask Deleted",
                            body = "${subtask.title} has been deleted",
                            type = NotificationType.SUBTASK_DELETED,
                            data = mapOf("taskId" to parentTaskId, "subtaskId" to subtaskId)
                        )
                    }
                }

                // Send FCM event for reactive updates
                val taskDoc = tasksCollection.document(parentTaskId).get().await()
                val task = Task.fromFirestore(taskDoc)
                val allUserIds = (task.assignedUserIds + task.creatorId + subtask.assignedUserIds).distinct()
                notificationService.sendUpdateEvent(
                    userIds = allUserIds,
                    eventType = "SUBTASK_DELETED",
                    data = mapOf(
                        "taskId" to parentTaskId,
                        "subtaskId" to subtaskId
                    )
                )

                // Recalculate parent task budget
                calculateTotalBudget(parentTaskId)
                updateParentTaskDueDate(parentTaskId)
                fetchSubtasks(parentTaskId)
                _success.value = "Subtask deleted successfully"
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}

