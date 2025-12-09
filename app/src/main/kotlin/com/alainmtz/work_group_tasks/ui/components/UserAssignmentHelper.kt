package com.alainmtz.work_group_tasks.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.alainmtz.work_group_tasks.domain.models.User
import com.alainmtz.work_group_tasks.ui.viewmodels.ChatViewModel

@Composable
fun UserAssignment( 
    assignedUsers: List<User>,
    onAssignUsers: (List<User>) -> Unit,
    isCreator: Boolean,
    chatViewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    var showAssignUserDialog by remember { mutableStateOf(false) }

    if (showAssignUserDialog) {
        AssignUserDialog(
            onDismiss = { showAssignUserDialog = false },
            onConfirm = { users ->
                onAssignUsers(users)
                showAssignUserDialog = false
            },
            chatViewModel = chatViewModel,
            initialSelection = assignedUsers
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        if (assignedUsers.isEmpty()) {
            Text("No users assigned", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            AvatarStack(users = assignedUsers, modifier = Modifier.clickable(enabled = isCreator) { showAssignUserDialog = true })
        }
        Spacer(modifier = Modifier.weight(1f))
        if (isCreator) {
            IconButton(onClick = { showAssignUserDialog = true }) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit Assigned Users")
            }
        }
    }
}

@Composable
fun AvatarStack(users: List<User>, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        users.take(4).forEachIndexed { index, user ->
            Box(modifier = Modifier.offset(x = (-10 * index).dp, y = 0.dp)) {
                Avatar(user = user)
            }
        }
        if (users.size > 4) {
            Box(
                modifier = Modifier
                    .offset(x = (-10 * 4).dp, y = 0.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${users.size - 4}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun Avatar(user: User, modifier: Modifier = Modifier) {
    if (user.photoUrl != null) {
        AsyncImage(
            model = user.photoUrl,
            contentDescription = user.name,
            modifier = modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.name?.take(1) ?: "?",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun AssignUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<User>) -> Unit,
    chatViewModel: ChatViewModel,
    initialSelection: List<User>
) {
    var searchQuery by remember { mutableStateOf("") }
    val foundUsers by chatViewModel.foundUsers.collectAsState()
    var selectedUsers by remember { mutableStateOf(initialSelection.toSet()) }

    val displayList = (selectedUsers + foundUsers).distinctBy { it.id }

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
                    items(displayList) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    selectedUsers = if (selectedUsers.any { it.id == user.id }) {
                                        selectedUsers.filter { it.id != user.id }.toSet()
                                    } else {
                                        selectedUsers + user
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedUsers.any { it.id == user.id },
                                onCheckedChange = { _ ->
                                     selectedUsers = if (selectedUsers.any { it.id == user.id }) {
                                        selectedUsers.filter { it.id != user.id }.toSet()
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