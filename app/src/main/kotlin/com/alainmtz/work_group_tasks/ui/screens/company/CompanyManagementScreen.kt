package com.alainmtz.work_group_tasks.ui.screens.company

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alainmtz.work_group_tasks.domain.models.Company
import com.alainmtz.work_group_tasks.domain.models.CompanyRole
import com.alainmtz.work_group_tasks.domain.models.Plan
import com.alainmtz.work_group_tasks.domain.models.PlanTier
import com.alainmtz.work_group_tasks.domain.models.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: CompanyManagementViewModel = viewModel()
) {
    val company by viewModel.currentCompany.collectAsState()
    val plan by viewModel.currentPlan.collectAsState()
    val members by viewModel.members.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show messages
    LaunchedEffect(errorMessage, successMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    
    val isOwner = company?.ownerId == viewModel.currentUserId
    val isAdmin = company?.adminIds?.contains(viewModel.currentUserId) == true
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Company Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (isOwner) {
                        IconButton(onClick = { showEditNameDialog = true }) {
                            Icon(Icons.Default.Edit, "Edit company name")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isOwner || isAdmin) {
                FloatingActionButton(onClick = { showAddMemberDialog = true }) {
                    Icon(Icons.Default.PersonAdd, "Add member")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Company Info Card
            item {
                CompanyInfoCard(
                    company = company,
                    plan = plan
                )
            }
            
            // Members Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Members (${members.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            
            if (members.isEmpty() && !isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No members yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(members) { member ->
                    MemberCard(
                        member = member,
                        currentUserId = viewModel.currentUserId,
                        isOwner = isOwner,
                        isAdmin = isAdmin,
                        onRemove = { viewModel.removeMember(member.id, member.name ?: "Unknown") },
                        onChangeRole = { newRole -> 
                            viewModel.updateMemberRole(member.id, member.name ?: "Unknown", newRole) 
                        }
                    )
                }
            }
        }
    }
    
    // Dialogs
    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onConfirm = { email, role ->
                viewModel.addMember(email, role)
                showAddMemberDialog = false
            }
        )
    }
    
    if (showEditNameDialog && company != null) {
        EditCompanyNameDialog(
            currentName = company!!.name,
            onDismiss = { showEditNameDialog = false },
            onConfirm = { newName ->
                viewModel.updateCompanyName(newName)
                showEditNameDialog = false
            }
        )
    }
}

@Composable
private fun CompanyInfoCard(company: Company?, plan: Plan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        company?.name ?: "Loading...",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (company != null) {
                        Text(
                            "ID: ${company.id.takeLast(8)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Surface(
                    color = when (plan.tier) {
                        PlanTier.FREE -> MaterialTheme.colorScheme.surfaceVariant
                        PlanTier.PRO -> Color(0xFF4CAF50)
                        PlanTier.BUSINESS -> Color(0xFF2196F3)
                        PlanTier.ENTERPRISE -> Color(0xFF9C27B0)
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        plan.name,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            
            if (company != null) {
                InfoRow(
                    Icons.Default.Group, 
                    "Groups", 
                    "${company.groupsCount}",
                    MaterialTheme.colorScheme.onPrimaryContainer
                )
                InfoRow(
                    Icons.Default.Task, 
                    "Active Tasks", 
                    "${company.activeTasksCount}",
                    MaterialTheme.colorScheme.onPrimaryContainer
                )
                InfoRow(
                    Icons.Default.Storage, 
                    "Storage Used", 
                    "${company.storageUsedBytes / 1_000_000} MB",
                    MaterialTheme.colorScheme.onPrimaryContainer
                )
                InfoRow(
                    Icons.Default.PhotoCamera,
                    "Photos This Month",
                    "${company.photosUploadedThisMonth}",
                    MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector, 
    label: String, 
    value: String,
    tint: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            icon, 
            contentDescription = null, 
            modifier = Modifier.size(20.dp),
            tint = tint
        )
        Text(
            label, 
            style = MaterialTheme.typography.bodyMedium,
            color = tint,
            modifier = Modifier.weight(1f)
        )
        Text(
            value, 
            style = MaterialTheme.typography.bodyMedium, 
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

@Composable
private fun MemberCard(
    member: User,
    currentUserId: String,
    isOwner: Boolean,
    isAdmin: Boolean,
    onRemove: () -> Unit,
    onChangeRole: (CompanyRole) -> Unit
) {
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    member.name ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    member.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(4.dp))
                
                Surface(
                    color = when (member.role) {
                        CompanyRole.OWNER -> MaterialTheme.colorScheme.primaryContainer
                        CompanyRole.ADMIN -> MaterialTheme.colorScheme.secondaryContainer
                        CompanyRole.MEMBER -> MaterialTheme.colorScheme.surfaceVariant
                        null -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        member.role?.toString() ?: "MEMBER",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Actions (only if not current user and has permissions)
            if (member.id != currentUserId && (isOwner || isAdmin) && member.role != CompanyRole.OWNER) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isOwner) {
                        IconButton(onClick = { showRoleDialog = true }) {
                            Icon(Icons.Default.ManageAccounts, "Change role")
                        }
                    }
                    
                    IconButton(onClick = { showRemoveDialog = true }) {
                        Icon(
                            Icons.Default.PersonRemove, 
                            "Remove", 
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else if (member.id == currentUserId) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "You",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    
    // Confirmation dialogs
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Remove Member?") },
            text = { Text("Are you sure you want to remove ${member.name ?: "this member"} from the company? They will lose access to all company resources.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove()
                        showRemoveDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showRoleDialog) {
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            title = { Text("Change Role") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select new role for ${member.name ?: "this member"}:")
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    OutlinedButton(
                        onClick = {
                            onChangeRole(CompanyRole.ADMIN)
                            showRoleDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, null)
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Admin", fontWeight = FontWeight.Bold)
                            Text(
                                "Can manage members and settings",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    OutlinedButton(
                        onClick = {
                            onChangeRole(CompanyRole.MEMBER)
                            showRoleDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Person, null)
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Member", fontWeight = FontWeight.Bold)
                            Text(
                                "Can access company resources",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRoleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, CompanyRole) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(CompanyRole.MEMBER) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Email, null)
                    }
                )
                
                Text("Role:", style = MaterialTheme.typography.labelLarge)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedRole == CompanyRole.MEMBER,
                        onClick = { selectedRole = CompanyRole.MEMBER },
                        label = { Text("Member") },
                        leadingIcon = if (selectedRole == CompanyRole.MEMBER) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = selectedRole == CompanyRole.ADMIN,
                        onClick = { selectedRole = CompanyRole.ADMIN },
                        label = { Text("Admin") },
                        leadingIcon = if (selectedRole == CompanyRole.ADMIN) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(email, selectedRole) },
                enabled = email.isNotBlank()
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

@Composable
private fun EditCompanyNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Company Name") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Company Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank() && name != currentName
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
