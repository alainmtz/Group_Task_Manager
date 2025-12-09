package com.alainmtz.work_group_tasks.ui.screens.profile

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.alainmtz.work_group_tasks.ui.components.ImageCropEditor
import com.image.cropview.CropType
import com.image.cropview.EdgeType

@Composable
fun ImageCropScreen(
    imageUri: String,
    onCropDone: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    LaunchedEffect(Unit) {
        // Log available EdgeTypes to find the rectangular one
        try {
            val values = EdgeType.values().joinToString { it.name }
            android.util.Log.d("ImageCropDebug", "EdgeType values: $values")
            val cropValues = CropType.values().joinToString { it.name }
            android.util.Log.d("ImageCropDebug", "CropType values: $cropValues")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    ImageCropEditor(
        imageUri = Uri.parse(imageUri),
        cropType = CropType.PROFILE_CIRCLE,
        edgeType = EdgeType.CIRCULAR,
        onCropDone = onCropDone,
        onCancel = onCancel
    )
}
