package com.alainmtz.work_group_tasks.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alainmtz.work_group_tasks.domain.models.TaskMessage
import com.alainmtz.work_group_tasks.domain.models.Subtask
import com.alainmtz.work_group_tasks.ui.viewmodels.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.Calendar

@Composable
fun ChatScreen(
    taskId: String,
    viewModel: TaskViewModel = viewModel(),
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onNavigateToSubtask: ((String) -> Unit)? = null
) {
    val messages by viewModel.messages.collectAsState()
    val subtasks by viewModel.subtasks.collectAsState()
    val success by viewModel.success.collectAsState()
    val currentUserId = viewModel.currentUserId
    var messageText by remember { mutableStateOf("") }
    var showSubtaskPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Group messages by date
    val groupedMessages = remember(messages) {
        messages.groupBy { message ->
            val calendar = java.util.Calendar.getInstance()
            calendar.time = message.createdAt
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            calendar.time
        }.toSortedMap()
    }
    
    // Track collapsed state for each date
    val collapsedDates = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(taskId) {
        viewModel.fetchMessages(taskId)
        viewModel.fetchSubtasks(taskId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(success) {
        if (success != null) {
            snackbarHostState.showSnackbar(success ?: "Message sent")
            viewModel.clearSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groupedMessages.forEach { (date, messagesForDate) ->
                val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                val isCollapsed = collapsedDates[dateKey] ?: false
                
                item(key = "date_$dateKey") {
                    DateSeparator(
                        date = date,
                        messageCount = messagesForDate.size,
                        isCollapsed = isCollapsed,
                        onToggleCollapse = {
                            collapsedDates[dateKey] = !isCollapsed
                        }
                    )
                }
                
                if (!isCollapsed) {
                    items(
                        items = messagesForDate,
                        key = { message -> message.id }
                    ) { message ->
                        MessageItem(
                            message = message,
                            isCurrentUser = message.authorId == currentUserId,
                            subtasks = subtasks,
                            onSubtaskClick = onNavigateToSubtask
                        )
                    }
                }
            }
        }

        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showSubtaskPicker = true }) {
                    Icon(Icons.Default.Link, contentDescription = "Reference Subtask")
                }
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(taskId, messageText)
                            messageText = ""
                        }
                    },
                    enabled = messageText.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
        }
        
        // Subtask Picker Dialog
        if (showSubtaskPicker) {
            AlertDialog(
                onDismissRequest = { showSubtaskPicker = false },
                title = { Text("Reference Subtask") },
                text = {
                    LazyColumn {
                        items(subtasks) { subtask ->
                            TextButton(
                                onClick = {
                                    messageText += " [${subtask.title}](#${subtask.id})"
                                    showSubtaskPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = subtask.title,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSubtaskPicker = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}

@Composable
fun DateSeparator(
    date: Date,
    messageCount: Int,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val messageDate = Calendar.getInstance().apply { time = date }
    
    val displayText = when {
        today.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == messageDate.get(Calendar.DAY_OF_YEAR) -> "Today"
        yesterday.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == messageDate.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
        else -> dateFormat.format(date)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onToggleCollapse() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isCollapsed) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isCollapsed) "Expand" else "Collapse",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$messageCount message${if (messageCount != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun MessageItem(
    message: TaskMessage,
    isCurrentUser: Boolean,
    subtasks: List<Subtask>,
    onSubtaskClick: ((String) -> Unit)? = null
) {
    // Parse message for subtask references [Title](#id)
    val regex = """\[([^\]]+)\]\(#([^)]+)\)""".toRegex()
    val matches = regex.findAll(message.text).toList()
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        if (!isCurrentUser) {
            Text(
                text = message.authorName,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }
        
        Surface(
            color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (matches.isEmpty()) {
                    // Normal message
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Message with subtask references
                    var lastIndex = 0
                    matches.forEach { match ->
                        // Text before reference
                        if (match.range.first > lastIndex) {
                            Text(
                                text = message.text.substring(lastIndex, match.range.first),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Subtask reference chip (clickable)
                        val subtaskTitle = match.groupValues[1]
                        val subtaskId = match.groupValues[2]
                        val subtask = subtasks.find { it.id == subtaskId }
                        
                        Surface(
                            color = if (isCurrentUser) 
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) 
                            else 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .clickable(enabled = subtask != null && onSubtaskClick != null) {
                                    onSubtaskClick?.invoke(subtaskId)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = subtaskTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        lastIndex = match.range.last + 1
                    }
                    
                    // Text after last reference
                    if (lastIndex < message.text.length) {
                        Text(
                            text = message.text.substring(lastIndex),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    
                    // Message status indicator (only for current user's messages)
                    if (isCurrentUser) {
                        when (message.status) {
                            "sending" -> {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = "Sending",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                            }
                            "sent" -> {
                                Icon(
                                    Icons.Default.Done,
                                    contentDescription = "Sent",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                            }
                            "delivered" -> {
                                Icon(
                                    Icons.Default.DoneAll,
                                    contentDescription = "Delivered",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                            }
                            "read" -> {
                                Icon(
                                    Icons.Default.DoneAll,
                                    contentDescription = "Read",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
