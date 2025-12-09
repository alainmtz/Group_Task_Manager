package com.alainmtz.work_group_tasks.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.alainmtz.work_group_tasks.MainActivity
import com.alainmtz.work_group_tasks.domain.services.UpdateEvent
import com.alainmtz.work_group_tasks.domain.services.UpdateEventBus
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val eventType = data["eventType"]
        
        Log.d("FCM", "Message received with eventType: $eventType")
        
        // Always emit event to UpdateEventBus for UI updates (when app is foreground)
        val event = UpdateEvent(
            eventType = eventType ?: "UNKNOWN",
            taskId = data["taskId"],
            subtaskId = data["subtaskId"],
            userId = data["userId"],
            groupId = data["groupId"],
            chatThreadId = data["chatThreadId"],
            data = data
        )
        serviceScope.launch {
            UpdateEventBus.emit(event)
            Log.d("FCM", "Event emitted for $eventType")
        }
        
        // Show notification if app is in background or notification payload exists
        if (remoteMessage.notification != null) {
            val title = remoteMessage.notification?.title ?: "Actualización"
            val body = remoteMessage.notification?.body ?: "Hay cambios en tus tareas"
            sendNotification(title, body, data)
        }
    }

    override fun onNewToken(token: String) {
        // If you want to send the token to your server, do it here.
        // We are already handling this in AuthViewModel, but this is good for token refreshes.
        // For now, we can just log it or rely on the app to update it on next launch/login.
    }

    private fun sendNotification(title: String?, messageBody: String?, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        
        // Add data to intent for deep linking
        data["taskId"]?.let { intent.putExtra("taskId", it) }
        data["subtaskId"]?.let { intent.putExtra("subtaskId", it) }
        data["eventType"]?.let { intent.putExtra("eventType", it) }
        
        // Generate unique notification ID based on taskId (to group by task)
        val notificationId = data["taskId"]?.hashCode() ?: 0

        val pendingIntent = PendingIntent.getActivity(
            this, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "task_updates"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app icon
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Actualizaciones de Tareas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones sobre cambios en tareas y subtareas"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
