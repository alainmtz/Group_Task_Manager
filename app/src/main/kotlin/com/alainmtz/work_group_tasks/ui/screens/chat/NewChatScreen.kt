package com.alainmtz.work_group_tasks.ui.screens.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alainmtz.work_group_tasks.domain.models.User
import com.alainmtz.work_group_tasks.ui.viewmodels.ChatViewModel
import com.alainmtz.work_group_tasks.ui.viewmodels.ContactsViewModel
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    onNavigateBack: () -> Unit,
    onChatCreated: (String) -> Unit,
    viewModel: ChatViewModel = viewModel(),
    contactsViewModel: ContactsViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val foundUsers by viewModel.foundUsers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val contacts by contactsViewModel.contacts.collectAsState()
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

    LaunchedEffect(contacts) {
        if (contacts.isNotEmpty() && searchQuery.isEmpty()) {
            viewModel.findUsersByPhoneContacts(contacts)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Chat") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                            contactsViewModel.syncContacts(context)
                        } else {
                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    }) {
                        if (isSyncingContacts) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Icon(Icons.Default.Contacts, contentDescription = "Sync Contacts")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    if (it.isEmpty()) {
                        if (contacts.isNotEmpty()) {
                            viewModel.findUsersByPhoneContacts(contacts)
                        } else {
                            // Clear if no contacts
                             viewModel.searchUsers("") 
                        }
                    } else {
                        viewModel.searchUsers(it)
                    }
                },
                label = { Text("Search by email or phone") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading || isSyncingContacts) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (foundUsers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isEmpty()) "Sync contacts or search by email/phone" else "No users found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Suggested from Contacts",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    LazyColumn {
                        items(foundUsers) { user ->
                            UserItem(
                                user = user,
                                onClick = {
                                    viewModel.createDirectChat(user) { threadId ->
                                        onChatCreated(threadId)
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
}

@Composable
fun UserItem(user: User, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(user.name ?: "Unknown") },
        supportingContent = { Text(user.email) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
