package com.alainmtz.work_group_tasks.ui.screens.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alainmtz.work_group_tasks.domain.models.Group
import com.alainmtz.work_group_tasks.domain.models.TaskPriority
import com.alainmtz.work_group_tasks.domain.models.User
import com.alainmtz.work_group_tasks.ui.viewmodels.ChatViewModel
import com.alainmtz.work_group_tasks.ui.viewmodels.GroupViewModel
import com.alainmtz.work_group_tasks.ui.viewmodels.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    onNavigateBack: () -> Unit,
    groupId: String? = null,
    viewModel: TaskViewModel = viewModel(),
    groupViewModel: GroupViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var dueDate by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val groups by groupViewModel.groups.collectAsState()
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var showGroupDropdown by remember { mutableStateOf(false) }

    var assignedUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var showAssignUserDialog by remember { mutableStateOf(false) }

    LaunchedEffect(groupId, groups) {
        if (groupId != null && groups.isNotEmpty()) {
            selectedGroup = groups.find { it.id == groupId }
        }
    }

    LaunchedEffect(Unit) {
        if (groups.isEmpty()) {
            groupViewModel.fetchUserGroups()
        }
    }

    val isLoading by viewModel.isLoading.collectAsState()

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate.time
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dueDate = Date(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showAssignUserDialog) {
        AssignUserDialog(
            onDismiss = { showAssignUserDialog = false },
            onConfirm = { selectedUsers ->
                assignedUsers = selectedUsers
                showAssignUserDialog = false
            },
            chatViewModel = chatViewModel,
            initialSelection = assignedUsers
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Task") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            ExposedDropdownMenuBox(
                expanded = showGroupDropdown,
                onExpandedChange = { showGroupDropdown = !showGroupDropdown },
            ) {
                OutlinedTextField(
                    value = selectedGroup?.name ?: "Personal Task",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Assign to Group") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showGroupDropdown) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = showGroupDropdown,
                    onDismissRequest = { showGroupDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Personal Task") },
                        onClick = { selectedGroup = null; showGroupDropdown = false }
                    )
                    groups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            onClick = { selectedGroup = group; showGroupDropdown = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Assigned Users", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (assignedUsers.isEmpty()) {
                    Text("No users assigned", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("${assignedUsers.size} users assigned", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { showAssignUserDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Assign Users")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Priority", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskPriority.values().forEach { p ->
                    FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.name) })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(dueDate),
                onValueChange = { },
                label = { Text("Due Date") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    viewModel.createTask(
                        title = title,
                        description = description,
                        dueDate = dueDate,
                        priority = priority,
                        groupId = selectedGroup?.id,
                        assignedUserIds = assignedUsers.map { it.id },
                        onSuccess = onNavigateBack
                    )
                },
                enabled = title.isNotEmpty() && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Create Task")
                }
            }
        }
    }
}

@Composable
private fun AssignUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<User>) -> Unit,
    chatViewModel: ChatViewModel,
    initialSelection: List<User>
) {
    var searchQuery by remember { mutableStateOf("") }
    val foundUsers by chatViewModel.foundUsers.collectAsState()
    var selectedUsers by remember { mutableStateOf(initialSelection.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Users") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        chatViewModel.searchUsers(it)
                    },
                    label = { Text("Search by email or phone") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(foundUsers) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    selectedUsers = if (selectedUsers.contains(user)) {
                                        selectedUsers - user
                                    } else {
                                        selectedUsers + user
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedUsers.contains(user),
                                onCheckedChange = { _ ->
                                    selectedUsers = if (selectedUsers.contains(user)) {
                                        selectedUsers - user
                                    } else {
                                        selectedUsers + user
                                    }
                                }
                            )
                            Text(user.name ?: user.email)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedUsers.toList()) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
