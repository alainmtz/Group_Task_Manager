package com.alainmtz.work_group_tasks.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alainmtz.work_group_tasks.domain.models.Company
import com.alainmtz.work_group_tasks.domain.models.Plan
import com.alainmtz.work_group_tasks.domain.models.PlanTier
import com.alainmtz.work_group_tasks.ui.components.UsageMetricsCard
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen for managing billing and subscription
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    company: Company,
    plan: Plan,
    onUpgradeClick: () -> Unit,
    onCancelSubscription: () -> Unit,
    onUpdatePaymentMethod: () -> Unit,
    onBackClick: () -> Unit
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Billing & Subscription") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Current plan card
            CurrentPlanCard(
                plan = plan,
                company = company,
                onUpgradeClick = onUpgradeClick
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Usage metrics
            UsageMetricsCard(
                company = company,
                plan = plan,
                onUpgradeClick = onUpgradeClick
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Billing details (if not free plan)
            if (plan.tier != PlanTier.FREE) {
                BillingDetailsCard(
                    company = company,
                    onUpdatePaymentMethod = onUpdatePaymentMethod
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Plan features
            PlanFeaturesCard(plan = plan)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Danger zone (cancel subscription)
            if (plan.tier != PlanTier.FREE) {
                DangerZoneCard(
                    onCancelClick = { showCancelDialog = true }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // Cancel subscription confirmation dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F)
                )
            },
            title = { Text("Cancel Subscription?") },
            text = {
                Text(
                    "Are you sure you want to cancel your ${plan.name} subscription? " +
                    "You'll lose access to premium features at the end of your billing period."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        onCancelSubscription()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text("Cancel Subscription")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Subscription")
                }
            }
        )
    }
}

@Composable
private fun CurrentPlanCard(
    plan: Plan,
    company: Company,
    onUpgradeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Current Plan",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                if (plan.tier != PlanTier.ENTERPRISE) {
                    FilledTonalButton(
                        onClick = onUpgradeClick,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upgrade")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Pricing display
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (plan.priceMonthly > 0) "$${plan.priceMonthly}" else "Free",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (plan.priceMonthly > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "/month",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Subscription status
            if (plan.tier != PlanTier.FREE) {
                Spacer(modifier = Modifier.height(12.dp))
                
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val renewalDate = dateFormat.format(company.nextBillingDate ?: Date())
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Next billing: $renewalDate",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BillingDetailsCard(
    company: Company,
    onUpdatePaymentMethod: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Payment method display (placeholder - will integrate with Google Play Billing)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Google Play",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Managed through Play Store",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                TextButton(onClick = onUpdatePaymentMethod) {
                    Text("Manage")
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Billing history
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Billing History",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                TextButton(onClick = { /* Open billing history */ }) {
                    Text("View")
                }
            }
        }
    }
}

@Composable
private fun PlanFeaturesCard(plan: Plan) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Plan Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // List key features
            FeatureItem(
                icon = Icons.Default.Group,
                text = if (plan.features.maxGroups == -1) "Unlimited groups" 
                       else "${plan.features.maxGroups} groups"
            )
            
            FeatureItem(
                icon = Icons.Default.Task,
                text = if (plan.features.maxActiveTasks == -1) "Unlimited active tasks"
                       else "${plan.features.maxActiveTasks} active tasks"
            )
            
            FeatureItem(
                icon = Icons.Default.People,
                text = if (plan.features.maxMembersPerGroup == -1) "Unlimited members per group"
                       else "${plan.features.maxMembersPerGroup} members per group"
            )
            
            FeatureItem(
                icon = Icons.Default.Storage,
                text = if (plan.features.maxStorageGB > 0) {
                    "${plan.features.maxStorageGB}GB storage"
                } else {
                    "${plan.features.maxStorageMB}MB storage"
                }
            )
            
            if (plan.features.canUseAnalytics) {
                FeatureItem(
                    icon = Icons.Default.Analytics,
                    text = "Advanced analytics"
                )
            }
            
            if (plan.features.canWhiteLabel) {
                FeatureItem(
                    icon = Icons.Default.Palette,
                    text = "Custom branding"
                )
            }
            
            if (plan.features.canUseAPI) {
                FeatureItem(
                    icon = Icons.Default.Api,
                    text = "API access"
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DangerZoneCard(onCancelClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Danger Zone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Canceling your subscription will downgrade you to the Free plan at the end of your billing period.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5D2C2C)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFD32F2F)
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFD32F2F))
                )
            ) {
                Text("Cancel Subscription")
            }
        }
    }
}
