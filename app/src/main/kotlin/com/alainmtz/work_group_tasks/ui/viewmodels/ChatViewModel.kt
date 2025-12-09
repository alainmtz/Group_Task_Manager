package com.alainmtz.work_group_tasks.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alainmtz.work_group_tasks.domain.models.ChatMessage
import com.alainmtz.work_group_tasks.domain.models.Attachment
import com.alainmtz.work_group_tasks.domain.models.ChatThread
import com.alainmtz.work_group_tasks.domain.models.ChatType
import com.alainmtz.work_group_tasks.domain.models.User
import com.alainmtz.work_group_tasks.ui.viewmodels.Contact
import com.alainmtz.work_group_tasks.domain.models.NotificationType
import com.alainmtz.work_group_tasks.domain.services.NotificationService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class ChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationService = NotificationService()
    private val chatThreadsCollection = db.collection("chatThreads")

    private val _chatThreads = MutableStateFlow<List<ChatThread>>(emptyList())
    val chatThreads: StateFlow<List<ChatThread>> = _chatThreads.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _totalUnreadCount = MutableStateFlow(0)
    val totalUnreadCount: StateFlow<Int> = _totalUnreadCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _foundUsers = MutableStateFlow<List<User>>(emptyList())
    val foundUsers: StateFlow<List<User>> = _foundUsers.asStateFlow()

    private var messagesListener: ListenerRegistration? = null

    init {
        fetchChatThreads()
    }

    fun searchUsers(query: String) {
        if (query.isEmpty()) {
            _foundUsers.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                // Search by email
                val emailSnapshot = db.collection("users")
                    .whereGreaterThanOrEqualTo("email", query)
                    .whereLessThanOrEqualTo("email", query + "\uf8ff")
                    .limit(10)
                    .get()
                    .await()

                // Search by phone number (if query is a number)
                val phoneSnapshot = if (query.all { it.isDigit() }) {
                    db.collection("users")
                        .whereGreaterThanOrEqualTo("phoneNumber", query)
                        .whereLessThanOrEqualTo("phoneNumber", query + "\uf8ff")
                        .limit(10)
                        .get()
                        .await()
                } else {
                    null
                }

                val emailUsers = emailSnapshot.documents.map { User.fromFirestore(it) }
                val phoneUsers = phoneSnapshot?.documents?.map { User.fromFirestore(it) } ?: emptyList()

                val combinedUsers = (emailUsers + phoneUsers).distinctBy { it.id } // Combine and remove duplicates
                
                // Filter out current user
                val currentUserId = auth.currentUser?.uid
                _foundUsers.value = combinedUsers.filter { it.id != currentUserId }
            } catch (e: Exception) {
                // Handle error
                 _error.value = e.message
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
                val allUsersSnapshot = db.collection("users").get().await()
                val allUsers = allUsersSnapshot.documents.map { User.fromFirestore(it) }
                val currentUserId = auth.currentUser?.uid

                val matchedUsers = allUsers.filter { user ->
                    if (user.id == currentUserId) return@filter false
                    
                    val userPhone = user.phoneNumber?.replace(Regex("[^0-9]"), "")
                    userPhone != null && normalizedContacts.any { contactPhone -> 
                        contactPhone.contains(userPhone) || userPhone.contains(contactPhone)
                    }
                }
                _foundUsers.value = matchedUsers
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun fetchChatThreads() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = chatThreadsCollection
                    .whereArrayContains("memberIds", userId)
                    .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val threads = snapshot.documents.map { ChatThread.fromFirestore(it) }
                _chatThreads.value = threads
                
                // Calculate total unread count
                _totalUnreadCount.value = threads.sumOf { thread ->
                    thread.unreadCounts[userId] ?: 0
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMessages(threadId: String) {
        messagesListener?.remove()
        _messages.value = emptyList()
        
        // Mark as read
        markThreadAsRead(threadId)

        messagesListener = chatThreadsCollection.document(threadId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = e.message
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val msgs = snapshot.documents.map { ChatMessage.fromFirestore(it) }
                    _messages.value = msgs
                }
            }
    }

    private fun markThreadAsRead(threadId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // We need to update the map key "unreadCounts.userId" to 0
                // Firestore allows updating nested fields using dot notation
                chatThreadsCollection.document(threadId)
                    .update("unreadCounts.$userId", 0)
                    .await()
                
                // Refresh threads to update UI count
                fetchChatThreads()
            } catch (e: Exception) {
                // Handle error silently or log
            }
        }
    }

    fun sendMessage(threadId: String, text: String, attachments: List<Attachment> = emptyList()) {
        val userId = auth.currentUser?.uid ?: return
        if (text.isBlank() && attachments.isEmpty()) return

        viewModelScope.launch {
            try {
                val message = hashMapOf(
                    "senderId" to userId,
                    "text" to text,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "readBy" to listOf(userId),
                    "attachments" to attachments.map { 
                        mapOf(
                            "url" to it.url,
                            "type" to it.type,
                            "name" to it.name
                        )
                    }
                )

                // Add message to subcollection
                chatThreadsCollection.document(threadId)
                    .collection("messages")
                    .add(message)
                    .await()

                // Update thread details and increment unread counts for others
                val threadDoc = chatThreadsCollection.document(threadId).get().await()
                val currentUnreadCounts = (threadDoc.get("unreadCounts") as? Map<String, Long>)?.toMutableMap() ?: mutableMapOf()
                val memberIds = (threadDoc.get("memberIds") as? List<String>) ?: emptyList()
                
                val displayMessage = if (text.isNotBlank()) text else "Sent an attachment"

                val updates = mutableMapOf<String, Any>(
                    "lastMessage" to displayMessage,
                    "lastMessageTimestamp" to FieldValue.serverTimestamp()
                )

                // Increment unread count for all other members
                memberIds.forEach { memberId ->
                    if (memberId != userId) {
                        val currentCount = currentUnreadCounts[memberId] ?: 0
                        updates["unreadCounts.$memberId"] = currentCount + 1
                    }
                }

                chatThreadsCollection.document(threadId)
                    .update(updates)
                    .await()
                
                // Notify other members
                val thread = _chatThreads.value.find { it.id == threadId }
                if (thread != null) {
                    val recipients = thread.memberIds.filter { it != userId }
                    if (recipients.isNotEmpty()) {
                        // Fetch sender name for better notification
                        val senderName = auth.currentUser?.displayName ?: "Someone"
                        notificationService.notifyTaskMembers(
                            memberIds = recipients,
                            title = senderName,
                            body = displayMessage,
                            type = NotificationType.MESSAGE_RECEIVED,
                            data = mapOf("threadId" to threadId)
                        )
                    }
                }

                // Refresh threads list to update order/preview
                fetchChatThreads()

            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun createDirectChat(otherUser: User, onSuccess: (String) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Check if thread already exists
                val existingThread = _chatThreads.value.find { thread ->
                    thread.type == ChatType.PRIVATE && 
                    thread.memberIds.contains(otherUser.id) && 
                    thread.memberIds.contains(userId)
                }

                if (existingThread != null) {
                    onSuccess(existingThread.id)
                    return@launch
                }

                // Create new thread
                val newThread = hashMapOf(
                    "memberIds" to listOf(userId, otherUser.id),
                    "lastMessage" to "",
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "type" to ChatType.PRIVATE.name,
                    "title" to otherUser.name // Optional: could be dynamic based on viewer
                )

                val docRef = chatThreadsCollection.add(newThread).await()
                onSuccess(docRef.id)
                fetchChatThreads()

            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadFile(uri: Uri, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val fileName = UUID.randomUUID().toString()
        val fileRef = storageRef.child("chat-images/$fileName")

        fileRef.putFile(uri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }
            }
            .addOnFailureListener {
                onError(it.message ?: "Upload failed")
            }
    }

    fun deleteChatThread(threadId: String, threadName: String = "Chat") {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get thread details to notify participants
                val threadDoc = chatThreadsCollection.document(threadId).get().await()
                val thread = ChatThread.fromFirestore(threadDoc)
                
                // Delete all messages in the thread
                val messagesSnapshot = db.collection("messages")
                    .whereEqualTo("chatThreadId", threadId)
                    .get()
                    .await()
                
                messagesSnapshot.documents.forEach { doc ->
                    doc.reference.delete().await()
                }

                // Delete the chat thread
                chatThreadsCollection.document(threadId).delete().await()

                // Notify all participants
                val currentUserId = auth.currentUser?.uid
                thread.memberIds.forEach { userId ->
                    if (userId != currentUserId) {
                        viewModelScope.launch {
                            notificationService.createNotificationAsync(
                                userId = userId,
                                title = "Chat Deleted",
                                body = "The chat '$threadName' has been deleted",
                                type = NotificationType.MESSAGE_RECEIVED,
                                data = mapOf("chatThreadId" to threadId)
                            )
                        }
                    }
                }

                // Send FCM event for reactive updates
                notificationService.sendUpdateEvent(
                    userIds = thread.memberIds.filter { it != currentUserId },
                    eventType = "CHAT_DELETED",
                    data = mapOf("chatThreadId" to threadId)
                )

                // Refresh chat threads list
                fetchChatThreads()
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
    }
}
