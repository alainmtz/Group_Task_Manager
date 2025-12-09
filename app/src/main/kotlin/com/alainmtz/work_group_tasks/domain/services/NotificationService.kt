package com.alainmtz.work_group_tasks.domain.services

import android.util.Log
import com.alainmtz.work_group_tasks.domain.models.NotificationType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.util.Date

class NotificationService {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val functions = FirebaseFunctions.getInstance()
    private val collectionPath = "notifications"

    fun notifyTaskMembers(
        memberIds: List<String>,
        title: String,
        body: String,
        type: NotificationType,
        data: Map<String, String?>
    ) {
        for (userId in memberIds) {
            createNotification(userId, title, body, type, data)
        }
    }

    suspend fun createNotificationAsync(
        userId: String,
        title: String,
        body: String,
        type: NotificationType,
        data: Map<String, String?>
    ) {
        try {
            Log.d("NotificationService", "createNotificationAsync: Creating notification for user $userId")
            val notification = hashMapOf(
                "userId" to userId,
                "title" to title,
                "body" to body,
                "type" to type.value, // Store enum as string
                "createdAt" to Timestamp(Date()),
                "isRead" to false,
                "data" to data
            )

            val result = db.collection(collectionPath)
                .add(notification)
                .await()
            
            Log.d("NotificationService", "createNotificationAsync: Notification saved with ID: ${result.id}")
        } catch (e: Exception) {
            Log.e("NotificationService", "createNotificationAsync: Error adding notification", e)
            throw e
        }
    }

    fun createNotification(
        userId: String,
        title: String,
        body: String,
        type: NotificationType,
        data: Map<String, String?>
    ) {
        val notification = hashMapOf(
            "userId" to userId,
            "title" to title,
            "body" to body,
            "type" to type.value, // Store enum as string
            "createdAt" to Timestamp(Date()),
            "isRead" to false,
            "data" to data
        )

        db.collection(collectionPath)
            .add(notification)
            .addOnSuccessListener { documentReference ->
                Log.d("NotificationService", "Notification added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e("NotificationService", "Error adding notification: ", e)
            }
    }
    
    /**
     * Send silent FCM event to trigger UI updates without notification
     */
    suspend fun sendUpdateEvent(
        userIds: List<String>,
        eventType: String,
        data: Map<String, String>
    ) {
        try {
            val eventData = hashMapOf(
                "userIds" to userIds,
                "eventType" to eventType,
                "data" to data
            )
            
            functions.getHttpsCallable("sendUpdateEvent")
                .call(eventData)
                .await()
                
            Log.d("NotificationService", "Update event sent: $eventType to ${userIds.size} users")
        } catch (e: Exception) {
            Log.e("NotificationService", "Error sending update event", e)
            // Don't throw - this is best-effort
        }
    }

    fun markAsRead(notificationId: String) {
        db.collection(collectionPath).document(notificationId)
            .update("isRead", true)
            .addOnFailureListener { e -> Log.e("NotificationService", "Error marking as read", e) }
    }

    fun markAllAsRead(userId: String) {
        db.collection(collectionPath)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                for (document in documents) {
                    batch.update(document.reference, "isRead", true)
                }
                batch.commit().addOnFailureListener { e -> Log.e("NotificationService", "Error marking all as read", e) }
            }
            .addOnFailureListener { e -> Log.e("NotificationService", "Error getting unread notifications", e) }
    }

    fun markAsUnread(notificationId: String) {
        db.collection(collectionPath).document(notificationId)
            .update("isRead", false)
            .addOnFailureListener { e -> Log.e("NotificationService", "Error marking as unread", e) }
    }

    fun deleteNotification(notificationId: String) {
        db.collection(collectionPath).document(notificationId)
            .delete()
            .addOnFailureListener { e -> Log.e("NotificationService", "Error deleting notification", e) }
    }

    fun saveTokenToFirestore(token: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val updates = hashMapOf(
                "fcmToken" to token,
                "fcmTokenUpdatedAt" to Timestamp.now()
            )
            db.collection("users").document(currentUser.uid)
                .update(updates as Map<String, Any>)
                .addOnSuccessListener { Log.d("NotificationService", "FCM token saved") }
                .addOnFailureListener { e -> Log.e("NotificationService", "Error saving FCM token", e) }
        } else {
            Log.e("NotificationService", "Cannot save FCM token: No user logged in")
        }
    }
}