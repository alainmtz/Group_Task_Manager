package com.alainmtz.work_group_tasks.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.image.cropview.ImageCrop
import com.image.cropview.CropType
import com.image.cropview.EdgeType
import java.io.File
import java.io.FileOutputStream

// Helper function to load bitmap
fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT < 28) {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        } else {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Helper function to save bitmap
fun saveBitmapToCache(context: Context, bitmap: Bitmap, prefix: String = "cropped"): Uri? {
    return try {
        val file = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropEditor(
    imageUri: Uri,
    cropType: CropType,
    edgeType: EdgeType,
    onCropDone: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageCrop by remember { mutableStateOf<ImageCrop?>(null) }

    LaunchedEffect(imageUri) {
        bitmap = getBitmapFromUri(context, imageUri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Image") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                imageCrop?.let { crop ->
                    val croppedBitmap = crop.onCrop()
                    if (croppedBitmap != null) {
                        val savedUri = saveBitmapToCache(context, croppedBitmap)
                        if (savedUri != null) {
                            onCropDone(savedUri)
                        }
                    }
                }
            }) {
                Icon(Icons.Default.Check, contentDescription = "Save")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            bitmap?.let { bmp ->
                imageCrop = remember(bmp) { ImageCrop(bmp) }
                
                imageCrop?.ImageCropView(
                    modifier = Modifier.fillMaxSize(),
                    guideLineColor = Color.LightGray,
                    guideLineWidth = 2.dp,
                    edgeCircleSize = 5.dp,
                    showGuideLines = true,
                    cropType = cropType,
                    edgeType = edgeType
                )
            }
        }
    }
}
