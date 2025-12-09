package com.alainmtz.work_group_tasks.domain.models

import android.net.Uri

data class PendingUpload(
    val subtaskId: String,
    val subtaskTitle: String,
    val parentTaskId: String,
    val imageUri: Uri,
    val localCachePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: UploadStatus = UploadStatus.PENDING
)

enum class UploadStatus {
    PENDING,
    UPLOADING,
    SUCCESS,
    FAILED
}
