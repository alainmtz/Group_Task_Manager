package com.alainmtz.work_group_tasks.ui.screens.groups

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alainmtz.work_group_tasks.ui.viewmodels.GroupViewModel
import com.alainmtz.work_group_tasks.domain.services.CompanyPlanProvider
import com.alainmtz.work_group_tasks.domain.services.FeatureFlags
import com.alainmtz.work_group_tasks.domain.models.PlanTier
import com.alainmtz.work_group_tasks.ui.components.UpgradeSnackbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    viewModel: GroupViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val company = CompanyPlanProvider.currentCompany.collectAsState().value
    val plan = CompanyPlanProvider.currentPlan.collectAsState().value
    
    var showUpgradePrompt by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error as snackbar
    LaunchedEffect(error) {
        error?.let {
            if (it.contains("limit", ignoreCase = true)) {
                showUpgradePrompt = true
            } else {
                snackbarHostState.showSnackbar(it)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Group") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { 
            SnackbarHost(snackbarHostState)
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    viewModel.createGroup(name, onSuccess = onNavigateBack)
                },
                enabled = name.isNotEmpty() && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create Group")
                }
            }
            }
            
            // Show upgrade snackbar at the bottom when limit is reached
            if (showUpgradePrompt) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    UpgradeSnackbar(
                        message = error ?: "Group limit reached - upgrade to create more groups",
                        onUpgradeClick = { 
                            showUpgradePrompt = false
                            onNavigateToPaywall()
                        },
                        onDismiss = { showUpgradePrompt = false }
                    )
                }
            }
        }
    }
}
