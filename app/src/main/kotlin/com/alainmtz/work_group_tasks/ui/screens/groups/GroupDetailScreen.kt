package com.alainmtz.work_group_tasks.ui.screens.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import com.alainmtz.work_group_tasks.ui.screens.tasks.TaskItem
import com.alainmtz.work_group_tasks.ui.viewmodels.AuthViewModel
import com.alainmtz.work_group_tasks.ui.viewmodels.ContactsViewModel
import com.alainmtz.work_group_tasks.ui.viewmodels.GroupViewModel
import com.alainmtz.work_group_tasks.ui.viewmodels.TaskViewModel
import com.alainmtz.work_group_tasks.ui.components.UpgradeSnackbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    onNavigateBack: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit,
    onNavigateToCreateTask: (String) -> Unit, // Pass groupId to create task
    onNavigateToChatDetail: (String) -> Unit,
    onNavigateToPaywall: () -> Unit = {},
    groupViewModel: GroupViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    contactsViewModel: ContactsViewModel = viewModel()
) {
    LaunchedEffect(groupId) {
        groupViewModel.loadGroup(groupId)
        taskViewModel.fetchGroupTasks(groupId)
    }
    
    // Listen for FCM update events via Flow
    LaunchedEffect(groupId) {
        com.alainmtz.work_group_tasks.domain.services.UpdateEventBus.events.collectLatest { event ->
            if (event.groupId == groupId) {
                when (event.eventType) {
                    "GROUP_UPDATED",
                    "GROUP_MEMBER_ADDED",
                    "GROUP_MEMBER_REMOVED" -> {
                        // Reload group data
                        groupViewModel.loadGroup(groupId)
                    }
                }
            }
            // Also listen for task events in this group
            if (event.data["groupId"] == groupId) {
                when (event.eventType) {
                    "TASK_CREATED",
                    "TASK_UPDATED",
                    "TASK_DELETED",
                    "TASK_STATUS_CHANGED" -> {
                        taskViewModel.fetchGroupTasks(groupId)
                    }
                }
            }
        }
    }

    val groupState = groupViewModel.selectedGroup.collectAsState()
    val group = groupState.value
    val groupMembers by groupViewModel.groupMembers.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val isCreator = group?.creatorId == currentUser?.uid
    
    val tasks by taskViewModel.tasks.collectAsState()
    val isTasksLoading by taskViewModel.isLoading.collectAsState()
    
    val contacts by contactsViewModel.contacts.collectAsState()
    val foundUsers by groupViewModel.foundUsers.collectAsState()
    val error by groupViewModel.error.collectAsState()
    
    var showAddMemberSheet by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showUpgradePrompt by remember { mutableStateOf(false) }
    
    // Show upgrade prompt when member limit error occurs
    LaunchedEffect(error) {
        if (error != null && error!!.contains("Member limit reached", ignoreCase = true)) {
            showUpgradePrompt = true
        }
    }

    // Close AddMemberSheet when member limit error occurs
    LaunchedEffect(error) {
        if (error != null && error!!.contains("Member limit reached", ignoreCase = true)) {
            showAddMemberSheet = false
        }
    }

    if (showAddMemberSheet) {
        AddMemberBottomSheet(
            onDismiss = { showAddMemberSheet = false },
            onAddUser = { userId ->
                groupViewModel.addMember(groupId, userId) {
                    showAddMemberSheet = false
                }
            },
            onSearch = { query ->
                groupViewModel.searchUsers(query)
            },
            foundUsers = foundUsers,
            contacts = contacts,
            onSyncContacts = {
                groupViewModel.findUsersByPhoneContacts(contacts)
            }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Group") },
            text = { Text("Are you sure you want to leave this group?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        groupViewModel.leaveGroup(groupId) {
                            showLeaveDialog = false
                            onNavigateBack()
                        }
                    }
                ) {
                    Text("Leave", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group?.name ?: "Group Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        groupViewModel.getGroupChatThreadId(groupId) { threadId ->
                            onNavigateToChatDetail(threadId)
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Group Chat")
                    }
                    if (isCreator) {
                        IconButton(onClick = { showAddMemberSheet = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Member")
                        }
                    }
                    IconButton(onClick = { showLeaveDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Leave Group")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToCreateTask(groupId) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Group Task")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (group == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                // Group Info Header
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Group Code",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        text = group.code.ifEmpty { "No Code" },
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (group.code.isNotEmpty()) {
                                    val clipboardManager = LocalClipboardManager.current
                                    val context = LocalContext.current
                                    IconButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(group.code))
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code")
                                    }
                                    IconButton(onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "Join my group in Agenda Colaborativa with code: ${group.code}")
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                    }) {
                                        Icon(Icons.Default.Share, contentDescription = "Share Code")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Members: ${group.memberIds.size}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            groupMembers.forEach { member ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• ${member.name ?: member.email}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                    if (isCreator && member.id != currentUser?.uid) {
                                        IconButton(
                                            onClick = {
                                                groupViewModel.removeMember(groupId, member.id) {
                                                    // Member removed
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove Member",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                
                // Tasks List
                if (isTasksLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (tasks.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No tasks found")
                        }
                    }
                } else {
                    items(tasks) { task ->
                        TaskItem(task = task, onClick = { onNavigateToTaskDetail(task.id) })
                    }
                }
                } // Close LazyColumn
            } // Close else block
            
            // Upgrade Snackbar (sibling to if/else, inside outer Box)
            if (showUpgradePrompt) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    UpgradeSnackbar(
                        message = error ?: "You've reached your member limit. Upgrade to PRO to collaborate with up to 15 members per group.",
                        onUpgradeClick = {
                            showUpgradePrompt = false
                            groupViewModel.clearError()
                            onNavigateToPaywall()
                        },
                        onDismiss = {
                            showUpgradePrompt = false
                            groupViewModel.clearError()
                        }
                    )
                }
            }
        } // Close outer Box
    } // Close Scaffold content lambda
} // Close function

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberBottomSheet(
    onDismiss: () -> Unit,
    onAddUser: (String) -> Unit,
    onSearch: (String) -> Unit,
    foundUsers: List<com.alainmtz.work_group_tasks.domain.models.User>,
    contacts: List<com.alainmtz.work_group_tasks.ui.viewmodels.Contact>,
    onSyncContacts: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        if (contacts.isNotEmpty()) {
            onSyncContacts()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 400.dp)
        ) {
            Text(
                text = "Add Members",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    onSearch(it)
                },
                label = { Text("Search by email") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            if (foundUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "Type to search or see contacts below" else "No users found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn {
                    items(foundUsers) { user ->
                        ListItem(
                            headlineContent = { Text(user.name ?: "No Name") },
                            supportingContent = { Text(user.email) },
                            trailingContent = {
                                Button(onClick = { onAddUser(user.id) }) {
                                    Text("Add")
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
