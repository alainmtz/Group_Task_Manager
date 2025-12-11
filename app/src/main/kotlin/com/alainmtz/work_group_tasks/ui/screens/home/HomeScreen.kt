package com.alainmtz.work_group_tasks.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alainmtz.work_group_tasks.ui.screens.chat.ChatListScreen
import com.alainmtz.work_group_tasks.ui.viewmodels.ChatViewModel
import com.alainmtz.work_group_tasks.ui.screens.groups.GroupListScreen
import com.alainmtz.work_group_tasks.ui.screens.tasks.TaskListScreen
import com.alainmtz.work_group_tasks.ui.viewmodels.ContactsViewModel
import com.alainmtz.work_group_tasks.ui.viewmodels.GroupViewModel
import com.alainmtz.work_group_tasks.ui.viewmodels.TaskViewModel
import com.alainmtz.work_group_tasks.domain.services.CompanyPlanProvider
import com.alainmtz.work_group_tasks.ui.components.UsageSummaryChip
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTaskDetail: (String) -> Unit,
    onNavigateToCreateTask: () -> Unit,
    onNavigateToGroupDetail: (String) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToChatDetail: (String) -> Unit,
    onNavigateToNewChat: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    taskViewModel: TaskViewModel = viewModel(),
    groupViewModel: GroupViewModel = viewModel(),
    contactsViewModel: ContactsViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Personal, 1 = Groups, 2 = Messages
    var showJoinGroupDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var groupCodeToJoin by remember { mutableStateOf("") }

    val tasks by taskViewModel.tasks.collectAsState()
    val isTasksLoading by taskViewModel.isLoading.collectAsState()
    val taskError by taskViewModel.error.collectAsState()
    
    val filterPriority by taskViewModel.filterPriority.collectAsState()
    val filterDateRange by taskViewModel.filterDateRange.collectAsState()
    
    val groups by groupViewModel.groups.collectAsState()
    val isGroupsLoading by groupViewModel.isLoading.collectAsState()

    val totalUnreadCount by chatViewModel.totalUnreadCount.collectAsState()
    
    val company = CompanyPlanProvider.currentCompany.collectAsState().value
    val plan = CompanyPlanProvider.currentPlan.collectAsState().value

    val isSyncingContacts by contactsViewModel.isSyncing.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                contactsViewModel.syncContacts(context)
            }
        }
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            contactsViewModel.syncContacts(context)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    // Refresh tasks when tab changes
    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> taskViewModel.fetchUserTasks()
            1 -> groupViewModel.fetchUserGroups()
            2 -> chatViewModel.fetchChatThreads()
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            onDismiss = { showFilterSheet = false },
            currentPriority = filterPriority,
            currentDateRange = filterDateRange,
            onApply = { priority, dateRange ->
                taskViewModel.setPriorityFilter(priority)
                taskViewModel.setDateRangeFilter(dateRange?.first, dateRange?.second)
            }
        )
    }

    if (showJoinGroupDialog) {
        AlertDialog(
            onDismissRequest = { showJoinGroupDialog = false },
            title = { Text("Join Group") },
            text = {
                OutlinedTextField(
                    value = groupCodeToJoin,
                    onValueChange = { groupCodeToJoin = it.uppercase() },
                    label = { Text("Group Code") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (groupCodeToJoin.isNotEmpty()) {
                        groupViewModel.joinGroupByCode(groupCodeToJoin) {
                            showJoinGroupDialog = false
                            groupCodeToJoin = ""
                        }
                    }
                }) {
                    Text("Join")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(when(selectedTab) {
                        0 -> "My Tasks"
                        1 -> "My Groups"
                        else -> "Messages"
                    }) 
                },
                actions = {
                    if (isSyncingContacts) {
                        Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Syncing Contacts",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (selectedTab == 0) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }
                    if (selectedTab == 1) {
                        TextButton(onClick = { showJoinGroupDialog = true }) {
                            Text("Join Group")
                        }
                    }
                    // Usage summary chip
                    if (company != null) {
                        UsageSummaryChip(
                            company = company!!,
                            plan = plan,
                            onClick = onNavigateToProfile
                        )
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Personal") },
                    label = { Text("Personal") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Group, contentDescription = "Groups") },
                    label = { Text("Groups") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = {
                        BadgedBox(
                            badge = {
                                if (totalUnreadCount > 0) {
                                    Badge { Text(totalUnreadCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Email, contentDescription = "Messages")
                        }
                    },
                    label = { Text("Messages") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> onNavigateToCreateTask()
                        1 -> onNavigateToCreateGroup()
                        2 -> onNavigateToNewChat()
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> {
                    if (taskError != null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text("Error: $taskError", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        TaskListScreen(
                            tasks = tasks,
                            isLoading = isTasksLoading,
                            onTaskClick = onNavigateToTaskDetail
                        )
                    }
                }
                1 -> {
                    GroupListScreen(
                        groups = groups,
                        isLoading = isGroupsLoading,
                        onGroupClick = onNavigateToGroupDetail
                    )
                }
                2 -> {
                    ChatListScreen(
                        onNavigateToChatDetail = onNavigateToChatDetail,
                        viewModel = chatViewModel
                    )
                }
            }
        }
    }
}
