package com.alainmtz.work_group_tasks.ui.screens.profile

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.alainmtz.work_group_tasks.domain.services.UpdateEventBus
import com.alainmtz.work_group_tasks.ui.components.Avatar
import kotlinx.coroutines.flow.collectLatest
import com.alainmtz.work_group_tasks.ui.settings.Theme
import com.alainmtz.work_group_tasks.ui.viewmodels.AuthViewModel
import com.alainmtz.work_group_tasks.ui.viewmodels.ThemeViewModel
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToImageCrop: (Uri) -> Unit,
    onNavigateToCompanyManagement: () -> Unit,
    onNavigateToBilling: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    authViewModel: AuthViewModel,
    themeViewModel: ThemeViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val error by authViewModel.error.collectAsState()
    val success by authViewModel.success.collectAsState()
    val theme by themeViewModel.theme.collectAsState()
    val completedSubtasksCount by authViewModel.completedSubtasksCount.collectAsState()
    val totalBudgetEarned by authViewModel.totalBudgetEarned.collectAsState()
    val budgetPending by authViewModel.budgetPending.collectAsState()
    val budgetReceived by authViewModel.budgetReceived.collectAsState()
    val budgetPaid by authViewModel.budgetPaid.collectAsState()
    val pendingEarnings by authViewModel.pendingEarnings.collectAsState()
    val paidOutDetails by authViewModel.paidOutDetails.collectAsState()

    var phoneNumber by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var showPendingEarningsDialog by remember { mutableStateOf(false) }
    var showPaidOutDialog by remember { mutableStateOf(false) }
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(error) {
        if (error != null) {
            errorMessage = error ?: ""
            showErrorDialog = true
        }
    }

    LaunchedEffect(success) {
        if (success != null) {
            snackbarHostState.showSnackbar(success ?: "Operation successful")
            authViewModel.clearSuccess()
        }
    }
    
    // Listen for FCM update events via Flow
    LaunchedEffect(Unit) {
        UpdateEventBus.events.collectLatest { event ->
            when (event.eventType) {
                "BUDGET_STATUS_CHANGED",
                "BUDGET_DISTRIBUTED",
                "EARNING_STATUS_CHANGED",
                "SUBTASK_COMPLETION_APPROVED" -> {
                    // Refresh budget stats and earnings
                    authViewModel.fetchUserStats()
                    authViewModel.fetchPendingEarnings()
                    authViewModel.fetchPaidOutDetails()
                }
                "USER_PROFILE_UPDATED" -> {
                    if (event.userId == authViewModel.currentUser.value?.uid) {
                        authViewModel.fetchUserProfile()
                    }
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onNavigateToImageCrop(uri)
            }
        }
    )
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && cameraPhotoUri != null) {
                onNavigateToImageCrop(cameraPhotoUri!!)
            }
        }
    )
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                launchCameraProfile(context) { uri ->
                    cameraPhotoUri = uri
                    cameraLauncher.launch(uri)
                }
            }
        }
    )
    
    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = { Text("Select Photo Source") },
            text = { Text("Choose where to get the photo from") },
            confirmButton = {
                TextButton(onClick = {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    showPhotoSourceDialog = false
                }) {
                    Text("Camera")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    showPhotoSourceDialog = false
                }) {
                    Text("Gallery")
                }
            }
        )
    }

    val hasPassword = remember(currentUser) {
        currentUser?.providerData?.any { it.providerId == "password" } == true
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    LaunchedEffect(userProfile) {
        userProfile?.phoneNumber?.let {
            phoneNumber = it
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false; newPassword = "" },
            title = { Text(if (hasPassword) "Change Password" else "Create Password") },
            text = {
                Column {
                    Text(if (hasPassword) "Enter your new password." else "Create a password for your account.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        authViewModel.updatePassword(newPassword) {
                            showPasswordDialog = false
                            newPassword = ""
                        }
                    },
                    enabled = newPassword.length >= 6
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false; newPassword = "" }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Pending Earnings Dialog
    if (showPendingEarningsDialog) {
        AlertDialog(
            onDismissRequest = { showPendingEarningsDialog = false },
            title = { Text("⏳ Pending Payments") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pendingEarnings.isEmpty()) {
                        item {
                            Text(
                                text = "No pending payments to confirm.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(pendingEarnings) { earning ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = earning.subtaskTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Amount: $${String.format("%.2f", earning.amount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            authViewModel.confirmBudgetReceipt(
                                                earningId = earning.id,
                                                creatorId = earning.creatorId,
                                                taskId = earning.taskId,
                                                amount = earning.amount,
                                                subtaskTitle = earning.subtaskTitle
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Confirm Receipt")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPendingEarningsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Paid Out Dialog
    if (showPaidOutDialog) {
        AlertDialog(
            onDismissRequest = { showPaidOutDialog = false },
            title = { Text("💳 Payments Made") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (paidOutDetails.isEmpty()) {
                        item {
                            Text(
                                text = "No confirmed payments yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(paidOutDetails) { detail ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // User Avatar
                                    if (detail.userPhotoUrl != null) {
                                        AsyncImage(
                                            model = detail.userPhotoUrl,
                                            contentDescription = detail.userName,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = detail.userName.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    
                                    // Payment Details
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = detail.userName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = detail.earning.subtaskTitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$${String.format("%.2f", detail.earning.amount)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                    }
                                    
                                    // Confirmed badge
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Confirmed",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPaidOutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    ThemeSelector(theme = theme, onThemeChange = { themeViewModel.setTheme(it) })
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 80.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            userProfile?.let {
                Box(modifier = Modifier.clickable { showPhotoSourceDialog = true }) {
                    Avatar(user = it, modifier = Modifier.size(120.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = currentUser?.displayName ?: userProfile?.name ?: "No Name",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentUser?.email ?: userProfile?.email ?: "No Email",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Statistics Cards - First Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Completed Subtasks Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$completedSubtasksCount",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Completed\nSubtasks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                // Total Budget Earned Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$${String.format("%.2f", totalBudgetEarned)}",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total\nEarned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Budget Status Cards - Second Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Budget Pending Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    onClick = { showPendingEarningsDialog = true }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$${String.format("%.2f", budgetPending)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "⏳ Pending",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                // Budget Received Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$${String.format("%.2f", budgetReceived)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "✅ Received",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                // Budget Paid Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ),
                    onClick = { 
                        authViewModel.fetchPaidOutDetails()
                        showPaidOutDialog = true 
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$${String.format("%.2f", budgetPaid)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💳 Paid Out",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it; isEditing = true },
                label = { Text("Phone Number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isEditing) {
                Button(
                    onClick = {
                        authViewModel.updatePhoneNumber(phoneNumber) { isEditing = false }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Phone Number")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = { showPasswordDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(if (hasPassword) "Change Password" else "Create Password")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Company Management Button
            Button(
                onClick = onNavigateToCompanyManagement,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("🏢 Manage Company")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Billing & Subscription Button
            Button(
                onClick = onNavigateToBilling,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Text("💳 Billing & Subscription")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    authViewModel.logout()
                    onSignOut()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Out")
            }
        }
    }
}

@Composable
fun ThemeSelector(theme: Theme, onThemeChange: (Theme) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Brightness4, contentDescription = "Theme")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Theme.values().forEach { themeOption ->
                DropdownMenuItem(
                    text = { Text(themeOption.name) },
                    onClick = {
                        onThemeChange(themeOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun launchCameraProfile(context: Context, onUriCreated: (Uri) -> Unit) {
    val photoFile = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
    val photoUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
    onUriCreated(photoUri)
}
