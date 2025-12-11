package com.alainmtz.work_group_tasks.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alainmtz.work_group_tasks.domain.models.Plan
import com.alainmtz.work_group_tasks.domain.models.PlanDefaults
import com.alainmtz.work_group_tasks.domain.models.PlanTier
import com.alainmtz.work_group_tasks.domain.services.CompanyService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Paywall screen showing plan comparison and upgrade options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    currentPlan: Plan,
    onUpgradeClick: (PlanTier) -> Unit,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isProcessing by remember { mutableStateOf(false) }
    
    // Function to handle upgrade with auto-company creation
    fun handleUpgrade(planTier: PlanTier) {
        if (planTier == PlanTier.FREE) {
            scope.launch {
                snackbarHostState.showSnackbar("You are already on the FREE plan")
            }
            return
        }
        
        isProcessing = true
        scope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                snackbarHostState.showSnackbar("Error: User not authenticated")
                isProcessing = false
                return@launch
            }
            
            // Check if user already has a company
            val hasCompany = com.alainmtz.work_group_tasks.domain.services.CompanyPlanProvider.currentCompany.value != null
            
            if (!hasCompany) {
                // Auto-create company for users upgrading from FREE
                val currentUser = FirebaseAuth.getInstance().currentUser
                val userName = currentUser?.displayName ?: "User"
                val userEmail = currentUser?.email ?: "unknown@email.com"
                val planId = "plan_${planTier.name.lowercase()}"
                
                try {
                    val result = CompanyService.createPersonalCompanyOnUpgrade(userId, userName, userEmail, planId)
                    result.onSuccess { company ->
                        snackbarHostState.showSnackbar("Company created and plan upgraded to ${planTier.name}!")
                        // TODO: Stage 3 - Integrate Google Play Billing here
                        // For now, company is created. In Stage 3, we'll add payment verification
                        onBackClick()
                    }.onFailure { error ->
                        snackbarHostState.showSnackbar("Error: ${error.message}")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error: ${e.message}")
                }
            } else {
                // User already has company, just trigger upgrade flow
                // TODO: Stage 3 - Integrate Google Play Billing here
                onUpgradeClick(planTier)
            }
            
            isProcessing = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upgrade Your Plan") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Choose the perfect plan for your team",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Currently on: ${currentPlan.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Plan Cards
            val plans = listOf(
                PlanDefaults.FREE,
                PlanDefaults.PRO,
                PlanDefaults.BUSINESS,
                PlanDefaults.ENTERPRISE
            )
            
            plans.forEach { plan ->
                PlanCard(
                    plan = plan,
                    isCurrentPlan = plan.tier == currentPlan.tier,
                    isRecommended = plan.tier == PlanTier.PRO,
                    onSelectClick = { handleUpgrade(plan.tier) },
                    isProcessing = isProcessing
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Feature Comparison Table
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Feature Comparison",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            FeatureComparisonTable()
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun PlanCard(
    plan: Plan,
    isCurrentPlan: Boolean,
    isRecommended: Boolean,
    onSelectClick: () -> Unit,
    isProcessing: Boolean = false
) {
    val borderColor = when {
        isRecommended -> MaterialTheme.colorScheme.primary
        isCurrentPlan -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isRecommended) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecommended) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Badge for recommended/current
            if (isRecommended || isCurrentPlan) {
                Surface(
                    color = if (isRecommended) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = if (isRecommended) "RECOMMENDED" else "CURRENT PLAN",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Plan name and price
            Text(
                text = plan.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                if (plan.priceMonthly > 0) {
                    Text(
                        text = "$${String.format("%.2f", plan.priceMonthly)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "/month",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                } else if (plan.tier == PlanTier.ENTERPRISE) {
                    Text(
                        text = "Custom",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Free",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Key features
            Spacer(modifier = Modifier.height(16.dp))
            
            val keyFeatures = getKeyFeatures(plan.tier)
            keyFeatures.forEach { feature ->
                FeatureItem(feature)
            }
            
            // CTA Button
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onSelectClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCurrentPlan && !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecommended) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = when {
                            isCurrentPlan -> "Current Plan"
                            plan.tier > PlanTier.FREE -> "Upgrade to ${plan.name}"
                            else -> "Downgrade to Free"
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun FeatureComparisonTable() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Feature",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Free",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(50.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Pro",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(50.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Business",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(70.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Enterprise",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(80.dp),
                    textAlign = TextAlign.Center
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Feature rows
            val features = listOf(
                ComparisonFeature("Groups", "1", "∞", "∞", "∞"),
                ComparisonFeature("Members/Group", "5", "15", "50", "∞"),
                ComparisonFeature("Active Tasks", "10", "50", "200", "∞"),
                ComparisonFeature("Storage", "100MB", "5GB", "50GB", "∞"),
                ComparisonFeature("Photos/Month", "5", "∞", "∞", "∞"),
                ComparisonFeature("Budgets & Bidding", "✗", "✓", "✓", "✓"),
                ComparisonFeature("Multimedia Chat", "✗", "✓", "✓", "✓"),
                ComparisonFeature("Analytics", "✗", "✓", "✓", "✓"),
                ComparisonFeature("Approval Hierarchy", "✗", "✗", "✓", "✓"),
                ComparisonFeature("Data Export", "✗", "✗", "✓", "✓"),
                ComparisonFeature("Audit Logs", "✗", "✗", "✓", "✓"),
                ComparisonFeature("SSO", "✗", "✗", "✗", "✓"),
                ComparisonFeature("API Access", "✗", "✗", "✗", "✓"),
                ComparisonFeature("White Label", "✗", "✗", "✗", "✓"),
                ComparisonFeature("Support", "Standard", "Priority", "Dedicated", "Manager")
            )
            
            features.forEach { feature ->
                ComparisonRow(feature)
                if (feature != features.last()) {
                    Divider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonRow(feature: ComparisonFeature) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = feature.free,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = feature.pro,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Center,
            color = if (feature.pro == "✓") MaterialTheme.colorScheme.primary else Color.Unspecified
        )
        Text(
            text = feature.business,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(70.dp),
            textAlign = TextAlign.Center,
            color = if (feature.business == "✓") MaterialTheme.colorScheme.primary else Color.Unspecified
        )
        Text(
            text = feature.enterprise,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.Center,
            color = if (feature.enterprise == "✓") MaterialTheme.colorScheme.primary else Color.Unspecified
        )
    }
}

private data class ComparisonFeature(
    val name: String,
    val free: String,
    val pro: String,
    val business: String,
    val enterprise: String
)

private fun getKeyFeatures(tier: PlanTier): List<String> {
    return when (tier) {
        PlanTier.FREE -> listOf(
            "1 group",
            "5 members per group",
            "10 active tasks",
            "100MB storage",
            "5 photos per month",
            "Standard support"
        )
        PlanTier.PRO -> listOf(
            "Unlimited groups",
            "15 members per group",
            "50 active tasks",
            "5GB storage",
            "Unlimited photos",
            "Budgets & bidding",
            "Multimedia chat",
            "Analytics dashboard",
            "Priority support"
        )
        PlanTier.BUSINESS -> listOf(
            "Unlimited groups",
            "50 members per group",
            "200 active tasks",
            "50GB storage",
            "Unlimited photos",
            "Approval hierarchy",
            "Data export",
            "Audit logs",
            "Advanced analytics",
            "Dedicated support"
        )
        PlanTier.ENTERPRISE -> listOf(
            "Unlimited everything",
            "SSO integration",
            "API access",
            "White labeling",
            "Custom integrations",
            "Dedicated account manager",
            "SLA 99.9%",
            "24/7 premium support"
        )
    }
}
