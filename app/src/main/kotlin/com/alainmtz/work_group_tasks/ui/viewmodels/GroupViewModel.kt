package com.alainmtz.work_group_tasks.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alainmtz.work_group_tasks.domain.models.Group
import com.alainmtz.work_group_tasks.domain.models.NotificationType
import com.alainmtz.work_group_tasks.domain.models.User
import com.alainmtz.work_group_tasks.domain.services.NotificationService
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

    fun fetchUserGroups() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = groupsCollection
                    .whereArrayContains("memberIds", userId)
                    .get()
                    .await()

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
                _groups.value = fetchedGroups
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createGroup(name: String, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val code = java.util.UUID.randomUUID().toString().substring(0, 6).uppercase()
                val newGroup = hashMapOf(
                    "name" to name,
                    "creatorId" to userId,
                    "memberIds" to listOf(userId),
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "code" to code
                )
                
                val groupRef = groupsCollection.add(newGroup).await()
                
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
        viewModelScope.launch {
            _isLoading.value = true
            try {
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
                groupsCollection.document(groupId)
                    .update("memberIds", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                    .await()
                
                updateChatThreadMembers(groupId, userId, false)
                
                // Refresh the group to update the UI
                loadGroup(groupId)
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun leaveGroup(groupId: String, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        removeMember(groupId, userId, onSuccess)
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
