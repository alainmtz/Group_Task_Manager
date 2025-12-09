package com.alainmtz.work_group_tasks.domain.services

import android.content.Context
import android.content.Intent

class DynamicLinkService {

    fun createAndShareInvitationLink(context: Context, groupId: String, groupName: String) {
        try {
            val invitationMessage = "I'm inviting you to join my group \"$groupName\" on " +
                "Collaborative Tasks!\n" +
                "Once you have the app, let me know and I'll add you."

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Invitation to join $groupName")
                putExtra(Intent.EXTRA_TEXT, invitationMessage)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share Invitation")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)

        } catch (e: Exception) {
            // In a real app, you'd want to log this error
            println("Error sharing invitation: $e")
        }
    }
}