package com.alainmtz.work_group_tasks.ui.screens.tasks

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.alainmtz.work_group_tasks.domain.models.*
import com.alainmtz.work_group_tasks.domain.services.UpdateEventBus
import kotlinx.coroutines.flow.collectLatest
import com.alainmtz.work_group_tasks.ui.components.UserAssignment
import com.alainmtz.work_group_tasks.ui.components.Avatar
import com.alainmtz.work_group_tasks.ui.components.UpgradeSnackbar
import com.alainmtz.work_group_tasks.ui.viewmodels.ChatViewModel
import com.alainmtz.work_group_tasks.ui.viewmodels.GroupViewModel
import com.alainmtz.work_group_tasks.ui.viewmodels.TaskViewModel
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.font.FontWeight
import com.alainmtz.work_group_tasks.domain.models.PendingUpload
import com.alainmtz.work_group_tasks.domain.models.UploadStatus
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit = {},
    viewModel: TaskViewModel = viewModel(),
    groupViewModel: GroupViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
) {
    val context = LocalContext.current
    val taskState = viewModel.getTaskFlow(taskId).collectAsState(initial = null)
    val task = taskState.value
    val subtasks by viewModel.subtasks.collectAsState()
    val success by viewModel.success.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pendingUploads by viewModel.pendingUploads.collectAsState()
    val upgradePrompt by viewModel.upgradePrompt.collectAsState()
    val currentUserId = viewModel.currentUserId
    
    val groups by groupViewModel.groups.collectAsState()
    val assignedUsers by groupViewModel.assignedUsers.collectAsState()
    var showAddSubtaskDialog by remember { mutableStateOf(false) }
    var showDeleteTaskDialog by remember { mutableStateOf(false) }
    var showDeleteSubtaskDialog by remember { mutableStateOf(false) }
    var subtaskToDelete by remember { mutableStateOf<Subtask?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(task) {
        task?.let { groupViewModel.fetchUsersByIds(it.assignedUserIds) }
    }
    
    LaunchedEffect(Unit) {
        if (groups.isEmpty()) {
            groupViewModel.fetchUserGroups()
        }
    }
    
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(taskId) {
        viewModel.fetchSubtasks(taskId)
    }
    
    // Listen for FCM update events via Flow
    LaunchedEffect(taskId) {
        UpdateEventBus.events.collectLatest { event ->
            // Only reload if event is for this task
            if (event.taskId == taskId || event.taskId == null) {
                when (event.eventType) {
                    "TASK_UPDATED",
                    "TASK_STATUS_CHANGED",
                    "TASK_ASSIGNMENT_CHANGED",
                    "SUBTASK_CREATED",
                    "SUBTASK_UPDATED",
                    "SUBTASK_DELETED",
                    "SUBTASK_STATUS_CHANGED",
                    "SUBTASK_ASSIGNMENT_CHANGED",
                    "SUBTASK_COMPLETION_APPROVED",
                    "SUBTASK_COMPLETION_REJECTED",
                    "BUDGET_STATUS_CHANGED",
                    "BUDGET_DISTRIBUTED",
                    "BID_PLACED",
                    "BID_ACCEPTED",
                    "BID_REJECTED" -> {
                        // Reload subtasks
                        viewModel.fetchSubtasks(taskId)
                    }
                }
            }
        }
    }
    
    LaunchedEffect(success) {
        if (success != null) {
            snackbarHostState.showSnackbar(success ?: "Operation successful")
            viewModel.clearSuccess()
        }
    }
    
    // Show upgrade prompt when photo limit reached
    UpgradeSnackbar(
        message = upgradePrompt ?: "",
        onDismiss = { viewModel.clearUpgradePrompt() },
        onUpgradeClick = {
            viewModel.clearUpgradePrompt()
            onNavigateToPaywall()
        }
    )
    
    if (showAddSubtaskDialog && task != null) {
        AddSubtaskDialog(
            onDismiss = { showAddSubtaskDialog = false },
            onConfirm = { title, description, dueDate, budget ->
                viewModel.addSubtask(task.id, title, description, dueDate, budget)
                showAddSubtaskDialog = false
            }
        )
    }

    // Delete Task Dialog
    // Navigate back when task is successfully deleted
    LaunchedEffect(viewModel.success.value) {
        if (viewModel.success.value?.contains("deleted successfully") == true) {
            onNavigateBack()
        }
    }

    if (showDeleteTaskDialog && task != null) {
        AlertDialog(
            onDismissRequest = { showDeleteTaskDialog = false },
            title = { Text("Delete Task?") },
            text = { Text("This will delete the task and all its subtasks. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTask(task.id)
                        showDeleteTaskDialog = false
                        // Navigate back immediately to avoid listener issues
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTaskDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Subtask Dialog
    if (showDeleteSubtaskDialog && subtaskToDelete != null && task != null) {
        AlertDialog(
            onDismissRequest = { showDeleteSubtaskDialog = false },
            title = { Text("Delete Subtask?") },
            text = { Text("This will delete \"${subtaskToDelete!!.title}\" and cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubtask(subtaskToDelete!!.id, task.id)
                        showDeleteSubtaskDialog = false
                        subtaskToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteSubtaskDialog = false
                    subtaskToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Column {
                            Text("Task Details")
                            Text("Total Budget: $${String.format("%.2f", task?.totalBudget ?: 0.0)}", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteTaskDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Task", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Details") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Chat") })
                }
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 80.dp)
            )
        }
    ) { paddingValues ->
        if (task == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (selectedTab == 0) {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(16.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = task.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        //PriorityChip(priority = task.priority)
                        //StatusChip(status = task.status)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Assigned Users Section
                    Text("Assigned Users", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    UserAssignment(
                        assignedUsers = assignedUsers,
                        onAssignUsers = { users -> viewModel.updateTaskAssignments(task.id, users.map { it.id }) {} },
                        isCreator = currentUserId == task.creatorId,
                        chatViewModel = chatViewModel
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Subtasks", style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { showAddSubtaskDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Subtask")
                        }
                    }
                    
                    // Pending uploads section
                    if (pendingUploads.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pending Uploads",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        pendingUploads.forEach { upload ->
                            PendingUploadCard(
                                upload = upload,
                                onRetry = { viewModel.retryPendingUpload(upload) },
                                onCancel = { viewModel.cancelPendingUpload(upload) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    subtasks.forEach { subtask ->
                        SubtaskItem(
                            subtask = subtask,
                            currentUserId = currentUserId,
                            isCreator = currentUserId == task.creatorId,
                            onToggleStatus = { viewModel.toggleSubtaskStatus(subtask) },
                            onCompleteWithProof = { uri -> viewModel.completeSubtaskWithProof(subtask, uri, context) },
                            onAssignUsers = { userIds -> viewModel.updateSubtaskAssignments(subtask.id, userIds, task.id) },
                            chatViewModel = chatViewModel,
                            groupViewModel = groupViewModel,
                            onPlaceBid = { amount -> viewModel.placeBid(subtask, amount) },
                            onAcceptBid = { bid -> viewModel.acceptBid(subtask, bid) },
                            onRequestPostponement = { date, reason -> viewModel.requestPostponement(subtask, date, reason) },
                            onRespondToPostponement = { request, accept -> viewModel.respondToPostponement(subtask, request, accept) },
                            onUpdateBudget = { budget -> viewModel.updateSubtaskBudget(subtask.id, budget, task.id) },
                            onUpdateDueDate = { date -> viewModel.updateSubtaskDueDate(subtask.id, date, task.id) },
                            onApproveCompletion = { viewModel.approveCompletion(subtask, task.id) },
                            onRejectCompletion = { viewModel.rejectCompletion(subtask, task.id) },
                            onDeleteSubtask = { 
                                subtaskToDelete = subtask
                                showDeleteSubtaskDialog = true
                            },
                            onRejectBid = { bid -> viewModel.rejectBid(subtask, bid) }
                        )
                    }
                    
                }
            } else {
                ChatScreen(
                    taskId = taskId, 
                    viewModel = viewModel, 
                    paddingValues = paddingValues,
                    onNavigateToSubtask = { subtaskId ->
                        // Switch to Subtasks tab when a subtask reference is clicked
                        selectedTab = 0
                    }
                )
            }
        }
        
        // Loading Overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Processing...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSubtaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, Date?, Double?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Date?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { 
                        dueDate = Date(it) 
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subtask") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it },
                    label = { Text("Budget (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dueDate?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it) } ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Due Date (optional)") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, description.ifBlank { null }, dueDate, budget.toDoubleOrNull()) },
                enabled = title.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtaskItem(
    subtask: Subtask,
    currentUserId: String?,
    isCreator: Boolean,
    onToggleStatus: () -> Unit,
    onCompleteWithProof: (android.net.Uri) -> Unit,
    onAssignUsers: (List<String>) -> Unit,
    onPlaceBid: (Double) -> Unit,
    onAcceptBid: (SubtaskBid) -> Unit,
    onRejectBid: (SubtaskBid) -> Unit,
    onRequestPostponement: (Date, String) -> Unit,
    onRespondToPostponement: (PostponementRequest, Boolean) -> Unit,
    onUpdateBudget: (Double) -> Unit,
    onUpdateDueDate: (Date) -> Unit,
    onApproveCompletion: () -> Unit,
    onRejectCompletion: () -> Unit,
    onDeleteSubtask: () -> Unit,
    chatViewModel: ChatViewModel,
    groupViewModel: GroupViewModel
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showBidDialog by remember { mutableStateOf(false) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var showEditDueDateDialog by remember { mutableStateOf(false) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var showPostponementDialog by remember { mutableStateOf(false) }
    var showFullScreenImage by remember { mutableStateOf(false) }
    var isPendingPhotoUpload by remember { mutableStateOf(false) }
    
    var subtaskAssignedUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onCompleteWithProof(uri)
                isPendingPhotoUpload = false
            } else {
                // User cancelled - reset checkbox
                isPendingPhotoUpload = false
            }
        }
    )
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && cameraPhotoUri != null) {
                onCompleteWithProof(cameraPhotoUri!!)
                isPendingPhotoUpload = false
            } else {
                // User cancelled - reset checkbox
                isPendingPhotoUpload = false
            }
        }
    )
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                launchCamera(context) { uri ->
                    cameraPhotoUri = uri
                    cameraLauncher.launch(uri)
                }
            } else {
                // User denied camera permission - reset checkbox
                isPendingPhotoUpload = false
            }
        }
    )

    LaunchedEffect(subtask.assignedUserIds) {
        subtaskAssignedUsers = groupViewModel.getUsersByIds(subtask.assignedUserIds)
    }

    if (showBidDialog) {
        BidDialog(
            onDismiss = { showBidDialog = false },
            onConfirm = {
                onPlaceBid(it)
                showBidDialog = false
            }
        )
    }

    if (showEditBudgetDialog) {
        EditBudgetDialog(
            initialValue = subtask.budget ?: 0.0,
            onDismiss = { showEditBudgetDialog = false },
            onConfirm = {
                onUpdateBudget(it)
                showEditBudgetDialog = false
            }
        )
    }
    
    if (showEditDueDateDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = subtask.dueDate?.time ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showEditDueDateDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { 
                        onUpdateDueDate(Date(it))
                    }
                    showEditDueDateDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDueDateDialog = false }) {
                    Text("Cancel")
                }
            }
        ) { DatePicker(state = datePickerState) }
    }
    
    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPhotoSourceDialog = false
                isPendingPhotoUpload = false // User cancelled dialog
            },
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

    if (showPostponementDialog) {
        PostponementDialog(
            currentDueDate = subtask.dueDate,
            onDismiss = { showPostponementDialog = false },
            onConfirm = { newDate, reason ->
                onRequestPostponement(newDate, reason)
                showPostponementDialog = false
            }
        )
    }
    
    // Full Screen Image Dialog
    if (showFullScreenImage && subtask.confirmationImageUrl != null) {
        Dialog(
            onDismissRequest = { showFullScreenImage = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { showFullScreenImage = false }
            ) {
                AsyncImage(
                    model = subtask.confirmationImageUrl,
                    contentDescription = "Completion Evidence Full Screen",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
                
                IconButton(
                    onClick = { showFullScreenImage = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = subtask.status == SubtaskStatus.COMPLETED || isPendingPhotoUpload,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            isPendingPhotoUpload = true
                            showPhotoSourceDialog = true
                        } else {
                            onToggleStatus()
                        }
                    },
                    enabled = subtask.assignedUserIds.contains(currentUserId)
                )
                Text(
                    text = subtask.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (subtask.status == SubtaskStatus.COMPLETED) TextDecoration.LineThrough else null
                )
                 Spacer(modifier = Modifier.weight(1f))
                 subtask.budget?.let { budget ->
                    if (budget > 0) {
                        Row(modifier = Modifier.clickable(enabled = isCreator) { showEditBudgetDialog = true }) {
                            Text(
                                text = "$${String.format("%.2f", budget)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (isCreator) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Budget", modifier = Modifier.size(16.dp))
                            }
                        }
                    } else if (isCreator) {
                        IconButton(onClick = { showEditBudgetDialog = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                                Icon(Icons.Default.AddCircle, contentDescription = "Add Budget", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("$", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                            }
                        }
                    }
                } ?: run {
                    if (isCreator) {
                        IconButton(onClick = { showEditBudgetDialog = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                                Icon(Icons.Default.AddCircle, contentDescription = "Add Budget", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("$", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                            }
                        }
                    }
                }
                if (isCreator) {
                    IconButton(onClick = { onDeleteSubtask() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Subtask", tint = MaterialTheme.colorScheme.error)
                    }
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand"
                    )
                }
            }

            if (isExpanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                    subtask.description?.let{
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Completion Evidence Thumbnail (visible to all members)
                    if (subtask.confirmationImageUrl != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFullScreenImage = true },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = subtask.confirmationImageUrl,
                                    contentDescription = "Completion Evidence Thumbnail",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.PhotoCamera,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = when (subtask.status) {
                                                SubtaskStatus.REVIEW -> "Completion Evidence (Under Review)"
                                                SubtaskStatus.COMPLETED -> "Completion Evidence (Approved)"
                                                else -> "Completion Evidence"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap to view full image",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Completion Review Section (Creator only, if status is REVIEW)
                    if (isCreator && subtask.status == SubtaskStatus.REVIEW && subtask.confirmationImageUrl != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Completion Pending Review", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Show confirmation image
                                AsyncImage(
                                    model = subtask.confirmationImageUrl,
                                    contentDescription = "Completion Evidence",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Accept/Reject Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onApproveCompletion() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Approve")
                                    }
                                    Button(
                                        onClick = { onRejectCompletion() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reject")
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // Due Date Section
                    if (isCreator) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showEditDueDateDialog = true }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Due Date", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    subtask.dueDate?.let { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(it) } ?: "Not set",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Icon(Icons.Default.Edit, contentDescription = "Edit Due Date", modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        subtask.dueDate?.let {
                            Text("Due: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(it)}", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    UserAssignment(
                        assignedUsers = subtaskAssignedUsers,
                        onAssignUsers = { users -> onAssignUsers(users.map { it.id }) },
                        isCreator = isCreator,
                        chatViewModel = chatViewModel
                    )
                    
                    if (subtask.assignedUserIds.contains(currentUserId) && subtask.status != SubtaskStatus.COMPLETED) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Bid Icon
                            if (subtask.bids.none { it.status == "accepted" } && subtask.bids.none { it.userId == currentUserId }) {
                                IconButton(onClick = { showBidDialog = true }) {
                                    Icon(Icons.Default.AttachMoney, contentDescription = "Place Bid", tint = MaterialTheme.colorScheme.primary)
                                }
                            } else if (subtask.bids.any { it.userId == currentUserId }) {
                                val userBid = subtask.bids.first { it.userId == currentUserId }
                                val bidColor = when (userBid.status) {
                                    "accepted" -> MaterialTheme.colorScheme.primary
                                    "rejected" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.tertiary
                                }
                                IconButton(onClick = { showBidDialog = true }) {
                                    Icon(Icons.Default.AttachMoney, contentDescription = "Bid ${userBid.status}", tint = bidColor)
                                }
                            }

                            // Postponement Icon
                            IconButton(onClick = { showPostponementDialog = true }) {
                                val hasAcceptedPostponement = subtask.postponementRequests.any { it.status == "accepted" && it.userId == currentUserId }
                                val hasPendingPostponement = subtask.postponementRequests.any { it.status == "pending" && it.userId == currentUserId }
                                val hasRejectedPostponement = subtask.postponementRequests.any { it.status == "rejected" && it.userId == currentUserId }
                                
                                val postponeColor = when {
                                    hasAcceptedPostponement -> MaterialTheme.colorScheme.primary
                                    hasRejectedPostponement -> MaterialTheme.colorScheme.error
                                    hasPendingPostponement -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Icon(Icons.Default.EventBusy, contentDescription = "Request Postponement", tint = postponeColor)
                            }
                        }
                    }
                    
                    if (isCreator && subtask.bids.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Bids:", style = MaterialTheme.typography.titleSmall)
                        subtask.bids.forEach { bid ->
                            val user = subtaskAssignedUsers.find { it.id == bid.userId }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    user?.let {
                                        Avatar(user = it, modifier = Modifier.size(40.dp))
                                    }
                                    Column {
                                        Text("${user?.name ?: "..."}", style = MaterialTheme.typography.bodyMedium)
                                        Text("$${bid.amount}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                if (bid.status == "pending") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { onAcceptBid(bid) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Accept",
                                                tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onRejectBid(bid) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Reject",
                                                tint = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Text(bid.status.uppercase(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 8.dp))
                                }
                            }
                        }
                    }
                    // Postponement Requests Section
                    if (isCreator && subtask.postponementRequests.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Postponement Requests:", style = MaterialTheme.typography.titleSmall)
                        subtask.postponementRequests.filter { it.status == "pending" }.forEach { request ->
                            val user = subtaskAssignedUsers.find { it.id == request.userId }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    user?.let {
                                        Avatar(user = it, modifier = Modifier.size(40.dp))
                                    }
                                    Column {
                                        Text("${user?.name ?: "..."}", style = MaterialTheme.typography.bodyMedium)
                                        Text("To: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(request.requestedDate)}", style = MaterialTheme.typography.labelSmall)
                                        request.reason.takeIf { it.isNotBlank() }?.let {
                                            Text("Reason: $it", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { onRespondToPostponement(request, true) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Accept Postponement",
                                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRespondToPostponement(request, false) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Reject Postponement",
                                            tint = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BidDialog(onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Place a Bid") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            Button(onClick = {
                amount.toDoubleOrNull()?.let(onConfirm)
            }) {
                Text("Bid")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostponementDialog(currentDueDate: Date?, onDismiss: () -> Unit, onConfirm: (Date, String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    var newDueDate by remember { mutableStateOf<Date?>(currentDueDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = newDueDate?.time ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { 
                        newDueDate = Date(it)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Postponement") },
        text = {
            Column {
                OutlinedTextField(
                    value = newDueDate?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it) } ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("New Due Date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                newDueDate?.let { onConfirm(it, reason) }
            }, enabled = newDueDate != null) {
                Text("Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditBudgetDialog(initialValue: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf(initialValue.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Budget") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            Button(onClick = {
                amount.toDoubleOrNull()?.let(onConfirm)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PendingUploadCard(
    upload: PendingUpload,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (upload.status) {
                UploadStatus.UPLOADING -> MaterialTheme.colorScheme.primaryContainer
                UploadStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status icon
                when (upload.status) {
                    UploadStatus.UPLOADING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    UploadStatus.FAILED -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Failed",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Pending",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = upload.subtaskTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (upload.status) {
                            UploadStatus.UPLOADING -> "Uploading evidence... (Attempt ${upload.retryCount + 1}/3)"
                            UploadStatus.FAILED -> "Upload failed - Image saved locally"
                            else -> "Waiting to upload"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (upload.status == UploadStatus.FAILED) {
                    IconButton(
                        onClick = onRetry,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry upload",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                if (upload.status != UploadStatus.UPLOADING) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel upload",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun launchCamera(context: Context, onUriCreated: (Uri) -> Unit) {
    val photoFile = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
    val photoUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
    onUriCreated(photoUri)
}