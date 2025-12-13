package com.alainmtz.work_group_tasks.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alainmtz.work_group_tasks.domain.models.Company
import com.alainmtz.work_group_tasks.domain.models.Group
import com.alainmtz.work_group_tasks.domain.models.NotificationType
import com.alainmtz.work_group_tasks.domain.models.PlanTier
import com.alainmtz.work_group_tasks.domain.models.User
import com.alainmtz.work_group_tasks.domain.services.NotificationService
import com.alainmtz.work_group_tasks.domain.services.CompanyPlanProvider
import com.alainmtz.work_group_tasks.domain.services.FeatureFlags
import com.alainmtz.work_group_tasks.domain.audit.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException

class GroupViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationService = NotificationService()
    private val auditLogger = AuditLogger(db)
    private val groupsCollection = db.collection("groups")
    private val chatThreadsCollection = db.collection("chatThreads")

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _assignedUsers = MutableStateFlow<List<User>>(emptyList())
    val assignedUsers: StateFlow<List<User>> = _assignedUsers.asStateFlow()

    init {
        fetchUserGroups()
    }
    
    fun clearError() {
        _error.value = null
    }
    
    suspend fun getUsersByIds(userIds: List<String>): List<User> {
        if (userIds.isEmpty()) {
            return emptyList()
        }
        return try {
            userIds.chunked(10).flatMap { chunk ->
                db.collection("users")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                    .get()
                    .await()
                    .documents
                    .map { User.fromFirestore(it) }
            }
        } catch (e: Exception) {
            _error.value = "Failed to fetch users: ${e.message}"
            emptyList()
        }
    }


    fun fetchUsersByIds(userIds: List<String>) {
        if (userIds.isEmpty()) {
            _assignedUsers.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                val users = userIds.chunked(10).flatMap { chunk ->
                    db.collection("users")
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                        .get()
                        .await()
                        .documents
                        .map { User.fromFirestore(it) }
                }
                _assignedUsers.value = users
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _error.value = "Failed to fetch assigned users: ${e.message}"
            }
        }
    }

    fun fetchUserGroups(forceRefresh: Boolean = false) {
        val userId = auth.currentUser?.uid ?: return
        android.util.Log.d("GroupViewModel", "fetchUserGroups called (forceRefresh=$forceRefresh) for user: $userId")
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val query = groupsCollection.whereArrayContains("memberIds", userId)
                val snapshot = if (forceRefresh) {
                    // Force fetch from server, not cache
                    android.util.Log.d("GroupViewModel", "Fetching from SERVER...")
                    query.get(com.google.firebase.firestore.Source.SERVER).await()
                } else {
                    query.get().await()
                }

                val fetchedGroups = snapshot.documents.map { doc ->
                    val group = Group.fromFirestore(doc)
                    // Handle legacy groups without code
                    if (group.code.isEmpty()) {
                        val newCode = java.util.UUID.randomUUID().toString().substring(0, 6).uppercase()
                        // Update Firestore asynchronously
                        launch {
                            try {
                                withContext(NonCancellable) {
                                    groupsCollection.document(group.id).update("code", newCode).await()
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                android.util.Log.e("GroupViewModel", "Failed to generate code for group ${group.id}", e)
                            }
                        }
                        // Return group with new code for immediate UI update
                        group.copy(code = newCode)
                    } else {
                        group
                    }
                }
                android.util.Log.d("GroupViewModel", "Fetched ${fetchedGroups.size} groups: ${fetchedGroups.map { it.name }}")
                _groups.value = fetchedGroups
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("GroupViewModel", "Error fetching groups", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createGroup(name: String, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        android.util.Log.d("GroupViewModel", "createGroup called with name: $name, current groups: ${_groups.value.size}")
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Check if user can create more groups
                val company = CompanyPlanProvider.currentCompany.value
                val plan = CompanyPlanProvider.currentPlan.value
                android.util.Log.d("GroupViewModel", "Limit check - company: ${company?.id}, plan: ${plan.tier}, current groups: ${_groups.value.size}")
                
                // If no company, create a virtual FREE company for limit checking
                val effectiveCompany = company ?: Company(
                    id = "temp",
                    planId = "free",
                    planTier = PlanTier.FREE,
                    groupsCount = _groups.value.size, // Current group count
                    ownerId = userId
                )
                
                val (canCreate, reason) = FeatureFlags.canCreateGroup(effectiveCompany, plan)
                android.util.Log.d("GroupViewModel", "Limit check result - canCreate: $canCreate, reason: $reason")
                if (!canCreate) {
                    android.util.Log.e("GroupViewModel", "GROUP LIMIT REACHED: $reason")
                    _error.value = reason ?: "Group limit reached - upgrade to create more groups"
                    _isLoading.value = false
                    return@launch
                }
                android.util.Log.d("GroupViewModel", "Limit check passed, creating group...")
                
                val code = java.util.UUID.randomUUID().toString().substring(0, 6).uppercase()
                val newGroup = hashMapOf(
                    "name" to name,
                    "creatorId" to userId,
                    "memberIds" to listOf(userId),
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "code" to code
                )
                
                val groupRef = groupsCollection.add(newGroup).await()
                
                // Update company groupsCount if user has a company
                if (company != null) {
                    db.collection("companies")
                        .document(company.id)
                        .update("groupsCount", com.google.firebase.firestore.FieldValue.increment(1))
                        .await()
                }
                
                // Create associated ChatThread
                val newThread = hashMapOf(
                    "memberIds" to listOf(userId),
                    "lastMessage" to "Group created",
                    "lastMessageTimestamp" to com.google.firebase.Timestamp.now(),
                    "type" to "group",
                    "title" to name,
                    "groupId" to groupRef.id
                )
                chatThreadsCollection.add(newThread).await()

                // Send FCM event
                notificationService.sendUpdateEvent(
                    userIds = listOf(userId),
                    eventType = "GROUP_CREATED",
                    data = mapOf("groupId" to groupRef.id)
                )

                fetchUserGroups()
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun joinGroupByCode(code: String, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = groupsCollection
                    .whereEqualTo("code", code)
                    .limit(1)
                    .get()
                    .await()

                if (snapshot.isEmpty) {
                    _error.value = "Invalid group code"
                    return@launch
                }

                val groupDoc = snapshot.documents.first()
                val currentMembers = groupDoc.get("memberIds") as? List<String> ?: emptyList()
                
                if (currentMembers.contains(userId)) {
                    _error.value = "You are already a member of this group"
                    return@launch
                }

                groupsCollection.document(groupDoc.id)
                    .update("memberIds", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                    .await()

                updateChatThreadMembers(groupDoc.id, userId, true)

                fetchUserGroups()
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _selectedGroup = MutableStateFlow<Group?>(null)
    val selectedGroup: StateFlow<Group?> = _selectedGroup.asStateFlow()

    private val _groupMembers = MutableStateFlow<List<User>>(emptyList())
    val groupMembers: StateFlow<List<User>> = _groupMembers.asStateFlow()

    private val _foundUsers = MutableStateFlow<List<User>>(emptyList())
    val foundUsers: StateFlow<List<User>> = _foundUsers.asStateFlow()

    fun loadGroup(groupId: String) {
        // Don't reload if already loaded
        if (_selectedGroup.value?.id == groupId) return
        
        _selectedGroup.value = null // Reset while loading
        _groupMembers.value = emptyList()
        viewModelScope.launch {
            try {
                val doc = groupsCollection.document(groupId).get().await()
                if (doc.exists()) {
                    var group = Group.fromFirestore(doc)
                    // Handle legacy groups without code
                    if (group.code.isEmpty()) {
                        val newCode = java.util.UUID.randomUUID().toString().substring(0, 6).uppercase()
                        try {
                            withContext(NonCancellable) {
                                groupsCollection.document(groupId).update("code", newCode).await()
                            }
                            group = group.copy(code = newCode)
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            android.util.Log.e("GroupViewModel", "Failed to generate code for group $groupId", e)
                        }
                    }
                    _selectedGroup.value = group
                    fetchGroupMembers(group.memberIds)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Handle error
            }
        }
    }

    private fun fetchGroupMembers(memberIds: List<String>) {
        if (memberIds.isEmpty()) {
            _groupMembers.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            try {
                // Firestore 'in' queries are limited to 10 items.
                val users = memberIds.chunked(10).flatMap { chunk ->
                    db.collection("users")
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                        .get()
                        .await()
                        .documents
                        .map { User.fromFirestore(it) }
                }
                _groupMembers.value = users
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Handle error
            }
        }
    }

    fun inviteMember(groupId: String, email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Find user by email
                val userSnapshot = db.collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .await()

                if (userSnapshot.isEmpty) {
                    _error.value = "User with email $email not found"
                    return@launch
                }

                val userId = userSnapshot.documents.first().getString("uid") ?: return@launch

                // 2. Add user to group
                groupsCollection.document(groupId)
                    .update("memberIds", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                    .await()

                updateChatThreadMembers(groupId, userId, true)

                // Notify the user
                val groupDoc = groupsCollection.document(groupId).get().await()
                val groupName = groupDoc.getString("name") ?: "a group"
                
                notificationService.createNotification(
                    userId = userId,
                    title = "Added to Group",
                    body = "You have been added to group: $groupName",
                    type = NotificationType.MEMBER_ASSIGNED,
                    data = mapOf("groupId" to groupId)
                )

                // Refresh group data if needed, or let the UI reload
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addMember(groupId: String, userId: String, onSuccess: () -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return
        android.util.Log.d("GroupViewModel", "addMember called for groupId: $groupId, userId: $userId")
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get current member count from group
                val groupDoc = groupsCollection.document(groupId).get().await()
                val currentMemberCount = (groupDoc.get("memberIds") as? List<*>)?.size ?: 0
                android.util.Log.d("GroupViewModel", "Current member count in group: $currentMemberCount")
                
                // Check if user can add more members (enforce limit for all users)
                val company = CompanyPlanProvider.currentCompany.value
                val plan = CompanyPlanProvider.currentPlan.value
                android.util.Log.d("GroupViewModel", "Limit check - company: ${company?.id}, plan: ${plan.tier}, current members: $currentMemberCount")
                
                val (canAdd, reason) = FeatureFlags.canAddMember(currentMemberCount, plan)
                android.util.Log.d("GroupViewModel", "Limit check result - canAdd: $canAdd, reason: $reason")
                
                if (!canAdd) {
                    android.util.Log.e("GroupViewModel", "MEMBER LIMIT REACHED: $reason")
                    _error.value = reason ?: "Member limit reached - upgrade to add more members"
                    _isLoading.value = false
                    return@launch
                }
                android.util.Log.d("GroupViewModel", "Limit check passed, adding member...")
                
                groupsCollection.document(groupId)
                    .update("memberIds", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                    .await()
                
                updateChatThreadMembers(groupId, userId, true)

                // Notify the user (reuse groupDoc from earlier)
                val groupName = groupDoc.getString("name") ?: "a group"
                
                notificationService.createNotification(
                    userId = userId,
                    title = "Added to Group",
                    body = "You have been added to group: $groupName",
                    type = NotificationType.MEMBER_ASSIGNED,
                    data = mapOf("groupId" to groupId)
                )

                // Send FCM event to all group members
                val group = Group.fromFirestore(groupDoc)
                notificationService.sendUpdateEvent(
                    userIds = group.memberIds,
                    eventType = "GROUP_MEMBER_ADDED",
                    data = mapOf(
                        "groupId" to groupId,
                        "userId" to userId
                    )
                )

                loadGroup(groupId)
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.isEmpty()) {
            _foundUsers.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                // Simple search by email prefix or exact match
                // Firestore doesn't support full text search natively.
                // We'll try to find by email first.
                val emailSnapshot = db.collection("users")
                    .whereGreaterThanOrEqualTo("email", query)
                    .whereLessThanOrEqualTo("email", query + "\uf8ff")
                    .limit(10)
                    .get()
                    .await()
                
                val users = emailSnapshot.documents.map { User.fromFirestore(it) }
                _foundUsers.value = users
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun findUsersByPhoneContacts(contacts: List<Contact>) {
        viewModelScope.launch {
            try {
                // Normalize contact numbers
                val normalizedContacts = contacts.mapNotNull { contact ->
                    contact.phoneNumber?.replace(Regex("[^0-9]"), "")
                }.filter { it.isNotEmpty() }

                if (normalizedContacts.isEmpty()) return@launch

                // Fetch all users (Optimization needed for large scale)
                // For now, fetch all and filter locally
                val allUsersSnapshot = db.collection("users").get().await()
                val allUsers = allUsersSnapshot.documents.map { User.fromFirestore(it) }

                val matchedUsers = allUsers.filter { user ->
                    val userPhone = user.phoneNumber?.replace(Regex("[^0-9]"), "")
                    userPhone != null && normalizedContacts.any { contactPhone -> 
                        // Check if one contains the other to handle country codes loosely
                        contactPhone.contains(userPhone) || userPhone.contains(contactPhone)
                    }
                }
                _foundUsers.value = matchedUsers
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun removeMember(groupId: String, userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                removeMemberInternal(groupId, userId)
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message
                android.util.Log.e("GroupViewModel", "Error removing member", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun removeMemberInternal(groupId: String, userId: String) {
        // Update chat thread FIRST (before removing from group)
        // This way the user still has permissions to query the chat thread
        updateChatThreadMembers(groupId, userId, false)
        
        // Then remove from group
        groupsCollection.document(groupId)
            .update("memberIds", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
            .await()
        
        // DON'T refresh the group if removing current user (they won't have permissions)
        val currentUserId = auth.currentUser?.uid
        if (userId != currentUserId) {
            // Only reload if removing someone else
            loadGroup(groupId)
        }
    }

    fun leaveGroup(groupId: String, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            var operationSuccessful = false
            try {
                // Get group to check member count and creator
                val groupDoc = groupsCollection.document(groupId).get().await()
                if (!groupDoc.exists()) {
                    _error.value = "Group not found"
                    auditLogger.log(AuditEvent(
                        userId = userId,
                        action = AuditAction.GROUP_LEAVE,
                        resource = AuditResource.GROUP,
                        resourceId = groupId,
                        status = AuditStatus.FAILED,
                        errorDetails = ErrorDetails(
                            errorType = "NotFound",
                            errorMessage = "Group does not exist",
                            canRetry = false
                        )
                    ))
                    return@launch
                }
                
                val group = Group.fromFirestore(groupDoc)
                val isCreator = group.creatorId == userId
                val memberCount = group.memberIds.size
                
                // If user is the only member or is the creator, delete the group
                if (memberCount <= 1 || isCreator) {
                    val result = deleteGroupInternal(groupId)
                    operationSuccessful = result.success
                } else {
                    // Just remove the member
                    removeMemberInternal(groupId, userId)
                    auditLogger.log(AuditEvent(
                        userId = userId,
                        action = AuditAction.GROUP_LEAVE,
                        resource = AuditResource.GROUP,
                        resourceId = groupId,
                        status = AuditStatus.SUCCESS,
                        metadata = mapOf(
                            "remainingMembers" to (memberCount - 1)
                        )
                    ))
                    operationSuccessful = true
                }
            } catch (e: Exception) {
                auditLogger.log(AuditEvent(
                    userId = userId,
                    action = AuditAction.GROUP_LEAVE,
                    resource = AuditResource.GROUP,
                    resourceId = groupId,
                    status = AuditStatus.FAILED,
                    errorDetails = ErrorDetails(
                        errorType = e::class.simpleName ?: "Unknown",
                        errorMessage = e.message ?: "Unknown error",
                        canRetry = true
                    )
                ))
                
                // Don't block navigation if it's a permission error (likely successful)
                if (e.message?.contains("PERMISSION_DENIED") != true) {
                    _error.value = e.message
                } else {
                    operationSuccessful = true
                }
            } finally {
                _isLoading.value = false
                // Refresh the groups list to remove the group from UI
                if (operationSuccessful) {
                    fetchUserGroups(forceRefresh = true)
                    onSuccess()
                }
            }
        }
    }

    fun deleteGroup(groupId: String, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Verify user is the creator
                val groupDoc = groupsCollection.document(groupId).get().await()
                if (!groupDoc.exists()) {
                    _error.value = "Group not found"
                    auditLogger.log(AuditEvent(
                        userId = userId,
                        action = AuditAction.GROUP_DELETE,
                        resource = AuditResource.GROUP,
                        resourceId = groupId,
                        status = AuditStatus.FAILED,
                        errorDetails = ErrorDetails(
                            errorType = "NotFound",
                            errorMessage = "Group does not exist",
                            canRetry = false
                        )
                    ))
                    return@launch
                }
                
                val group = Group.fromFirestore(groupDoc)
                if (group.creatorId != userId) {
                    _error.value = "Only the creator can delete this group"
                    auditLogger.log(AuditEvent(
                        userId = userId,
                        action = AuditAction.GROUP_DELETE,
                        resource = AuditResource.GROUP,
                        resourceId = groupId,
                        status = AuditStatus.FAILED,
                        errorDetails = ErrorDetails(
                            errorType = "Unauthorized",
                            errorMessage = "User is not the group creator",
                            canRetry = false
                        )
                    ))
                    return@launch
                }
                
                val result = deleteGroupInternal(groupId)
                if (result.success) {
                    onSuccess()
                }
            } catch (e: Exception) {
                _error.value = e.message
                android.util.Log.e("GroupViewModel", "Error deleting group", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun deleteGroupInternal(groupId: String): OperationResult {
        val userId = auth.currentUser?.uid ?: "unknown"
        val auditEvents = mutableListOf<AuditEvent>()
        var criticalSuccess = false
        var taskCount = 0
        var companyId: String? = null
        var companyPlan: String? = null
        val operationStartTime = System.currentTimeMillis()

        try {
            // Step 1: Get group metadata
            val groupDoc = groupsCollection.document(groupId).get().await()
            if (groupDoc.exists()) {
                val group = Group.fromFirestore(groupDoc)
                companyId = group.companyId
                
                // Get company plan for context
                if (companyId != null) {
                    try {
                        val companyDoc = db.collection("companies").document(companyId).get().await()
                        companyPlan = companyDoc.getString("plan") ?: "FREE"
                    } catch (e: Exception) {
                        // Plan info is optional
                    }
                }
            }
            
            // Create shared operation context
            val operationContext = OperationContext(
                companyId = companyId,
                companyPlan = companyPlan,
                sourceScreen = "GroupDetailScreen",
                triggerType = "USER_ACTION"
            )
            
            // Step 2: Delete chat thread (best effort - may fail due to permissions)
            val chatStartTime = System.currentTimeMillis()
            val chatEvent = try {
                val chatSnapshot = chatThreadsCollection
                    .whereEqualTo("groupId", groupId)
                    .limit(1)
                    .get()
                    .await()
                
                val chatDuration = System.currentTimeMillis() - chatStartTime
                
                if (!chatSnapshot.isEmpty) {
                    val chatId = chatSnapshot.documents.first().id
                    chatThreadsCollection.document(chatId).delete().await()
                    
                    AuditEvent(
                        userId = userId,
                        action = AuditAction.CHAT_DELETE,
                        resource = AuditResource.CHAT_THREAD,
                        resourceId = chatId,
                        status = AuditStatus.SUCCESS,
                        metadata = mapOf("groupId" to groupId),
                        metrics = OperationMetrics(
                            durationMs = chatDuration,
                            resourcesAffected = 1
                        ),
                        context = operationContext
                    )
                } else {
                    AuditEvent(
                        userId = userId,
                        action = AuditAction.CHAT_DELETE,
                        resource = AuditResource.CHAT_THREAD,
                        resourceId = "none",
                        status = AuditStatus.SKIPPED,
                        metadata = mapOf("reason" to "No chat thread found", "groupId" to groupId),
                        metrics = OperationMetrics(durationMs = chatDuration),
                        context = operationContext
                    )
                }
            } catch (e: Exception) {
                val chatDuration = System.currentTimeMillis() - chatStartTime
                AuditEvent(
                    userId = userId,
                    action = AuditAction.CHAT_DELETE,
                    resource = AuditResource.CHAT_THREAD,
                    resourceId = groupId,
                    status = if (e.message?.contains("PERMISSION_DENIED") == true) 
                        AuditStatus.PERMISSION_DENIED 
                    else 
                        AuditStatus.FAILED,
                    errorDetails = ErrorDetails(
                        errorType = e::class.simpleName ?: "Unknown",
                        errorMessage = e.message ?: "Unknown error",
                        canRetry = false,
                        affectedOperations = listOf("chatThread.delete")
                    ),
                    metrics = OperationMetrics(durationMs = chatDuration),
                    context = operationContext
                )
            }
            auditEvents.add(chatEvent)
            
            // Step 3: Delete tasks (best effort - may fail due to permissions)
            val taskStartTime = System.currentTimeMillis()
            val taskEvents = try {
                val tasksSnapshot = db.collection("tasks")
                    .whereEqualTo("groupId", groupId)
                    .get()
                    .await()
                
                taskCount = tasksSnapshot.documents.size
                val deletedTasks = mutableListOf<String>()
                
                tasksSnapshot.documents.forEach { taskDoc ->
                    try {
                        taskDoc.reference.delete().await()
                        deletedTasks.add(taskDoc.id)
                    } catch (e: Exception) {
                        // Log individual task deletion failure but continue
                    }
                }
                
                val taskDuration = System.currentTimeMillis() - taskStartTime
                
                if (deletedTasks.size == taskCount) {
                    listOf(AuditEvent(
                        userId = userId,
                        action = AuditAction.TASK_DELETE,
                        resource = AuditResource.TASK,
                        resourceId = groupId,
                        status = AuditStatus.SUCCESS,
                        metadata = mapOf(
                            "tasksDeleted" to deletedTasks.size,
                            "taskIds" to deletedTasks
                        ),
                        metrics = OperationMetrics(
                            durationMs = taskDuration,
                            resourcesAffected = deletedTasks.size
                        ),
                        context = operationContext
                    ))
                } else {
                    listOf(AuditEvent(
                        userId = userId,
                        action = AuditAction.TASK_DELETE,
                        resource = AuditResource.TASK,
                        resourceId = groupId,
                        status = AuditStatus.PARTIAL_SUCCESS,
                        metadata = mapOf(
                            "tasksDeleted" to deletedTasks.size,
                            "totalTasks" to taskCount
                        ),
                        metrics = OperationMetrics(
                            durationMs = taskDuration,
                            resourcesAffected = deletedTasks.size
                        ),
                        context = operationContext
                    ))
                }
            } catch (e: Exception) {
                val taskDuration = System.currentTimeMillis() - taskStartTime
                listOf(AuditEvent(
                    userId = userId,
                    action = AuditAction.TASK_DELETE,
                    resource = AuditResource.TASK,
                    resourceId = groupId,
                    status = if (e.message?.contains("PERMISSION_DENIED") == true) 
                        AuditStatus.PERMISSION_DENIED 
                    else 
                        AuditStatus.FAILED,
                    errorDetails = ErrorDetails(
                        errorType = e::class.simpleName ?: "Unknown",
                        errorMessage = e.message ?: "Unknown error",
                        canRetry = false,
                        affectedOperations = listOf("tasks.query", "tasks.delete")
                    ),
                    metrics = OperationMetrics(durationMs = taskDuration),
                    context = operationContext
                ))
            }
            auditEvents.addAll(taskEvents)
            
            // Step 4: Delete group (CRITICAL - must succeed)
            val groupStartTime = System.currentTimeMillis()
            try {
                groupsCollection.document(groupId).delete().await()
                criticalSuccess = true
                val groupDuration = System.currentTimeMillis() - groupStartTime
                
                auditEvents.add(AuditEvent(
                    userId = userId,
                    action = AuditAction.GROUP_DELETE,
                    resource = AuditResource.GROUP,
                    resourceId = groupId,
                    status = AuditStatus.SUCCESS,
                    metadata = mapOf(
                        "companyId" to (companyId ?: "unknown"),
                        "tasksAffected" to taskCount
                    ),
                    metrics = OperationMetrics(
                        durationMs = groupDuration,
                        resourcesAffected = 1
                    ),
                    context = operationContext
                ))
            } catch (e: Exception) {
                val groupDuration = System.currentTimeMillis() - groupStartTime
                auditEvents.add(AuditEvent(
                    userId = userId,
                    action = AuditAction.GROUP_DELETE,
                    resource = AuditResource.GROUP,
                    resourceId = groupId,
                    status = AuditStatus.FAILED,
                    errorDetails = ErrorDetails(
                        errorType = e::class.simpleName ?: "Unknown",
                        errorMessage = e.message ?: "Unknown error",
                        canRetry = true,
                        affectedOperations = listOf("group.delete")
                    ),
                    metrics = OperationMetrics(durationMs = groupDuration),
                    context = operationContext
                ))
                throw e // Critical failure - must propagate
            }
            
            // Step 5: Update company counters (best effort)
            if (companyId != null) {
                val counterStartTime = System.currentTimeMillis()
                val counterEvent = try {
                    db.collection("companies")
                        .document(companyId)
                        .update(
                            mapOf(
                                "groupsCount" to com.google.firebase.firestore.FieldValue.increment(-1),
                                "activeTasksCount" to com.google.firebase.firestore.FieldValue.increment(-taskCount.toLong())
                            )
                        )
                        .await()
                    
                    val counterDuration = System.currentTimeMillis() - counterStartTime
                    
                    AuditEvent(
                        userId = userId,
                        action = AuditAction.COUNTER_UPDATE,
                        resource = AuditResource.COMPANY_COUNTER,
                        resourceId = companyId,
                        status = AuditStatus.SUCCESS,
                        metadata = mapOf(
                            "groupsDecrement" to -1,
                            "tasksDecrement" to -taskCount
                        ),
                        metrics = OperationMetrics(
                            durationMs = counterDuration,
                            resourcesAffected = 2 // groupsCount + activeTasksCount
                        ),
                        context = operationContext
                    )
                } catch (e: Exception) {
                    val counterDuration = System.currentTimeMillis() - counterStartTime
                    AuditEvent(
                        userId = userId,
                        action = AuditAction.COUNTER_UPDATE,
                        resource = AuditResource.COMPANY_COUNTER,
                        resourceId = companyId,
                        status = AuditStatus.FAILED,
                        errorDetails = ErrorDetails(
                            errorType = e::class.simpleName ?: "Unknown",
                            errorMessage = e.message ?: "Unknown error",
                            canRetry = true,
                            affectedOperations = listOf("company.counters.update")
                        ),
                        metrics = OperationMetrics(durationMs = counterDuration),
                        context = operationContext
                    )
                }
                auditEvents.add(counterEvent)
            }
            
        } catch (e: Exception) {
            // Critical failure already logged in audit events
            if (!criticalSuccess) {
                throw e
            }
        }
        
        // Calculate total metrics
        val totalDuration = System.currentTimeMillis() - operationStartTime
        val totalResourcesAffected = auditEvents.sumOf { it.metrics?.resourcesAffected ?: 0 }
        
        // Generate operation result
        val summary = OperationSummary(
            totalOperations = auditEvents.size,
            successfulOperations = auditEvents.count { it.status == AuditStatus.SUCCESS },
            failedOperations = auditEvents.count { 
                it.status == AuditStatus.FAILED || it.status == AuditStatus.PERMISSION_DENIED 
            },
            skippedOperations = auditEvents.count { it.status == AuditStatus.SKIPPED },
            criticalOperationSuccess = criticalSuccess,
            totalDurationMs = totalDuration,
            resourcesAffected = totalResourcesAffected
        )
        
        val result = OperationResult(
            success = criticalSuccess,
            events = auditEvents,
            summary = summary
        )
        
        // Log complete audit trail
        auditLogger.logOperationResult(result)
        
        return result
    }

    fun getGroupChatThreadId(groupId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val snapshot = chatThreadsCollection
                    .whereEqualTo("groupId", groupId)
                    .limit(1)
                    .get()
                    .await()
                
                if (!snapshot.isEmpty) {
                    onSuccess(snapshot.documents.first().id)
                } else {
                    createMissingGroupChat(groupId, onSuccess)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private suspend fun createMissingGroupChat(groupId: String, onSuccess: (String) -> Unit) {
        try {
            val groupDoc = groupsCollection.document(groupId).get().await()
            if (!groupDoc.exists()) return
            
            val group = Group.fromFirestore(groupDoc)
            
            val newThread = hashMapOf(
                "memberIds" to group.memberIds,
                "lastMessage" to "Group chat initialized",
                "lastMessageTimestamp" to com.google.firebase.Timestamp.now(),
                "type" to "group",
                "title" to group.name,
                "groupId" to groupId
            )
            
            val ref = chatThreadsCollection.add(newThread).await()
            onSuccess(ref.id)
        } catch (e: Exception) {
            android.util.Log.e("GroupViewModel", "Failed to create missing group chat", e)
        }
    }

    private suspend fun updateChatThreadMembers(groupId: String, userId: String, isAdding: Boolean) {
        try {
            val snapshot = chatThreadsCollection
                .whereEqualTo("groupId", groupId)
                .limit(1)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val threadId = snapshot.documents.first().id
                if (isAdding) {
                    chatThreadsCollection.document(threadId)
                        .update("memberIds", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                        .await()
                } else {
                    chatThreadsCollection.document(threadId)
                        .update("memberIds", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                        .await()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("GroupViewModel", "Failed to update chat thread members", e)
        }
    }
}
