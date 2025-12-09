package com.alainmtz.work_group_tasks.ui.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alainmtz.work_group_tasks.domain.models.User
import com.alainmtz.work_group_tasks.domain.models.Earning
import com.alainmtz.work_group_tasks.domain.models.PaidOutDetail
import com.alainmtz.work_group_tasks.domain.models.Task
import com.alainmtz.work_group_tasks.domain.services.NotificationService
import com.alainmtz.work_group_tasks.domain.models.NotificationType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import com.google.firebase.messaging.FirebaseMessaging

import com.google.firebase.auth.GoogleAuthProvider

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()
    private val notificationService: NotificationService = NotificationService()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()
    
    private val _completedSubtasksCount = MutableStateFlow(0)
    val completedSubtasksCount: StateFlow<Int> = _completedSubtasksCount.asStateFlow()
    
    private val _totalBudgetEarned = MutableStateFlow(0.0)
    val totalBudgetEarned: StateFlow<Double> = _totalBudgetEarned.asStateFlow()
    
    private val _budgetPending = MutableStateFlow(0.0)
    val budgetPending: StateFlow<Double> = _budgetPending.asStateFlow()
    
    private val _budgetReceived = MutableStateFlow(0.0)
    val budgetReceived: StateFlow<Double> = _budgetReceived.asStateFlow()
    
    private val _budgetPaid = MutableStateFlow(0.0)
    val budgetPaid: StateFlow<Double> = _budgetPaid.asStateFlow()
    
    private val _pendingEarnings = MutableStateFlow<List<Earning>>(emptyList())
    val pendingEarnings: StateFlow<List<Earning>> = _pendingEarnings.asStateFlow()
    
    private val _paidOutDetails = MutableStateFlow<List<PaidOutDetail>>(emptyList())
    val paidOutDetails: StateFlow<List<PaidOutDetail>> = _paidOutDetails.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            if (firebaseAuth.currentUser != null) {
                fetchUserProfile()
                fetchUserStats()
                fetchPendingEarnings()
            } else {
                _userProfile.value = null
                _completedSubtasksCount.value = 0
                _totalBudgetEarned.value = 0.0
                _budgetPending.value = 0.0
                _budgetReceived.value = 0.0
                _budgetPaid.value = 0.0
            }
        }
    }

    fun confirmBudgetReceipt(earningId: String, creatorId: String, taskId: String, amount: Double, subtaskTitle: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Update earning status to confirmed
                db.collection("users")
                    .document(userId)
                    .collection("earnings")
                    .document(earningId)
                    .update("status", "confirmed")
                    .await()
                
                // Get current user name
                val userDoc = db.collection("users").document(userId).get().await()
                val userName = userDoc.getString("name") ?: "A user"
                
                // Send notification to creator
                notificationService.createNotificationAsync(
                    userId = creatorId,
                    title = "💳 Payment Confirmed",
                    body = "$userName confirmed receipt of $${String.format("%.2f", amount)} for \"$subtaskTitle\"",
                    type = NotificationType.BUDGET_PROPOSAL,
                    data = mapOf("taskId" to taskId)
                )
                
                // Send system message to task chat
                val systemMessage = mapOf(
                    "text" to "💳 $userName confirmed receipt of $${String.format("%.2f", amount)} for \"$subtaskTitle\"",
                    "authorId" to "system",
                    "timestamp" to com.google.firebase.Timestamp.now()
                )
                db.collection("tasks").document(taskId)
                    .collection("comments")
                    .add(systemMessage)
                    .await()
                
                // Send FCM update event to all task members
                val taskDoc = db.collection("tasks").document(taskId).get().await()
                val task = Task.fromFirestore(taskDoc)
                val allUserIds = (task.assignedUserIds + task.creatorId).distinct()
                notificationService.sendUpdateEvent(
                    userIds = allUserIds,
                    eventType = "EARNING_STATUS_CHANGED",
                    data = mapOf(
                        "taskId" to taskId,
                        "userId" to userId,
                        "amount" to amount.toString()
                    )
                )
                
                // Refresh stats
                fetchUserStats()
                fetchPendingEarnings()
                
                _success.value = "Payment confirmation sent successfully"
            } catch (e: Exception) {
                _error.value = "Failed to confirm payment: ${e.message}"
            }
        }
    }

    fun fetchUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    _userProfile.value = User.fromFirestore(doc)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updateProfilePicture(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val storageRef = storage.reference.child("avatars/$userId")
                val uploadTask = storageRef.putFile(uri).await()
                val downloadUrl = storageRef.downloadUrl.await()

                db.collection("users").document(userId)
                    .update("photoUrl", downloadUrl.toString())
                    .await()

                fetchUserProfile() // Refresh user profile
                _success.value = "Profile picture updated successfully"
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("App attestation failed") == true || 
                    e.message?.contains("Firebase App Check API") == true -> {
                        "Firebase App Check not configured. Please contact support."
                    }
                    e.message?.contains("Permission denied") == true || 
                    e.message?.contains("User does not have permission") == true -> {
                        "Storage access denied. This is a server configuration issue. Please contact support."
                    }
                    e.message?.contains("Too many attempts") == true -> {
                        "Too many upload attempts. Please wait 5 minutes before trying again."
                    }
                    e.message?.contains("The server has terminated") == true -> {
                        "Upload session was interrupted. Please try again."
                    }
                    else -> e.message ?: "Failed to upload profile picture"
                }
                _error.value = errorMsg
                android.util.Log.e("AuthViewModel", "Error updating profile picture: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePhoneNumber(phoneNumber: String, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                db.collection("users").document(uid)
                    .update("phoneNumber", phoneNumber)
                    .await()
                fetchUserProfile() // Refresh profile
                _success.value = "Phone number updated successfully"
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d("AuthViewModel", "Sending password reset email to: $email")
                auth.sendPasswordResetEmail(email).await()
                android.util.Log.d("AuthViewModel", "Password reset email sent successfully")
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Error sending password reset email", e)
                _error.value = e.message ?: "Failed to send reset email"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePassword(password: String, onSuccess: () -> Unit) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                user.updatePassword(password).await()
                _success.value = "Password updated successfully"
                onSuccess()
            } catch (e: Exception) {
                val errorMsg = if (e.message?.contains("Permission denied") == true) {
                    "Could not update password. Please try again."
                } else {
                    e.message ?: "Failed to update password"
                }
                _error.value = errorMsg
                android.util.Log.e("AuthViewModel", "Error updating password: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user
                if (user != null) {
                    // Check if user exists, if not create
                    val userDoc = db.collection("users").document(user.uid).get().await()
                    if (!userDoc.exists()) {
                        val userMap = hashMapOf(
                            "uid" to user.uid,
                            "email" to user.email,
                            "name" to user.displayName,
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )
                        db.collection("users").document(user.uid).set(userMap).await()
                    }
                    updateFcmToken()
                    onSuccess()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Google Sign-In failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                updateFcmToken()
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message ?: "Login failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(email: String, password: String, name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user
                if (user != null) {
                    // Create user document in Firestore
                    val userMap = hashMapOf(
                        "uid" to user.uid,
                        "email" to email,
                        "name" to name,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                    db.collection("users").document(user.uid).set(userMap).await()
                    updateFcmToken()
                    onSuccess()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Registration failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun updateFcmToken() {
        try {
            val token = messaging.token.await()
            val userId = auth.currentUser?.uid
            if (userId != null) {
                db.collection("users").document(userId)
                    .update("fcmToken", token)
                    .await()
            }
        } catch (e: Exception) {
            // Log error or handle silently
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun setError(message: String) {
        _error.value = message
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccess() {
        _success.value = null
    }
    
    fun fetchUserStats() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Get user earnings from the earnings collection (as recipient)
                val earningsSnapshot = db.collection("users")
                    .document(userId)
                    .collection("earnings")
                    .get()
                    .await()
                
                var totalEarned = 0.0
                var pendingAmount = 0.0
                var receivedAmount = 0.0
                val completedSubtaskIds = mutableSetOf<String>()
                
                for (doc in earningsSnapshot.documents) {
                    val amount = doc.getDouble("amount") ?: 0.0
                    val status = doc.getString("status") ?: "pending"
                    val subtaskId = doc.getString("subtaskId")
                    
                    totalEarned += amount
                    
                    if (status == "confirmed") {
                        receivedAmount += amount
                    } else {
                        pendingAmount += amount
                    }
                    
                    if (subtaskId != null) {
                        completedSubtaskIds.add(subtaskId)
                    }
                }
                
                // Get budgets paid by current user (as creator) - confirmed only
                val allUsersSnapshot = db.collection("users").get().await()
                var paidAmount = 0.0
                
                for (userDoc in allUsersSnapshot.documents) {
                    val userEarningsSnapshot = userDoc.reference
                        .collection("earnings")
                        .whereEqualTo("creatorId", userId)
                        .whereEqualTo("status", "confirmed")
                        .get()
                        .await()
                    
                    for (earningDoc in userEarningsSnapshot.documents) {
                        val amount = earningDoc.getDouble("amount") ?: 0.0
                        paidAmount += amount
                    }
                }
                
                _totalBudgetEarned.value = totalEarned
                _completedSubtasksCount.value = completedSubtaskIds.size
                _budgetPending.value = pendingAmount
                _budgetReceived.value = receivedAmount
                _budgetPaid.value = paidAmount
            } catch (e: Exception) {
                // Handle error silently or log it
                Log.e("AuthViewModel", "Error fetching user stats", e)
            }
        }
    }
    
    fun fetchPendingEarnings() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val earningsSnapshot = db.collection("users")
                    .document(userId)
                    .collection("earnings")
                    .whereEqualTo("status", "pending")
                    .get()
                    .await()
                
                val earnings = earningsSnapshot.documents.mapNotNull { doc ->
                    try {
                        Earning.fromFirestore(doc)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                _pendingEarnings.value = earnings
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error fetching pending earnings", e)
            }
        }
    }
    
    fun fetchPaidOutDetails() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val allUsersSnapshot = db.collection("users").get().await()
                val paidOutList = mutableListOf<PaidOutDetail>()
                
                for (userDoc in allUsersSnapshot.documents) {
                    val userEarningsSnapshot = userDoc.reference
                        .collection("earnings")
                        .whereEqualTo("creatorId", userId)
                        .whereEqualTo("status", "confirmed")
                        .get()
                        .await()
                    
                    for (earningDoc in userEarningsSnapshot.documents) {
                        try {
                            val earning = Earning.fromFirestore(earningDoc)
                            val userName = userDoc.getString("name") ?: "Unknown User"
                            val userPhotoUrl = userDoc.getString("photoUrl")
                            
                            paidOutList.add(
                                PaidOutDetail(
                                    earning = earning,
                                    userName = userName,
                                    userPhotoUrl = userPhotoUrl
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Error parsing paid out earning", e)
                        }
                    }
                }
                
                // Sort by timestamp descending (most recent first)
                _paidOutDetails.value = paidOutList.sortedByDescending { it.earning.timestamp.seconds }
                
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error fetching paid out details", e)
            }
        }
    }
}
