package com.alainmtz.work_group_tasks.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
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
import com.alainmtz.work_group_tasks.domain.models.Company
import com.alainmtz.work_group_tasks.domain.models.Plan
import com.alainmtz.work_group_tasks.domain.services.FeatureFlags

/**
 * Card displaying all usage metrics for the current plan
 */
@Composable
fun UsageMetricsCard(
    company: Company,
    plan: Plan,
    onUpgradeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Usage",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = plan.name,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Groups usage
            val maxGroups = if (plan.features.maxGroups == -1) null else plan.features.maxGroups
            UsageIndicator(
                icon = Icons.Default.Group,
                label = "Groups",
                current = company.groupsCount,
                max = maxGroups,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Tasks usage
            val maxTasks = if (plan.features.maxActiveTasks == -1) null else plan.features.maxActiveTasks
            UsageIndicator(
                icon = Icons.Default.Task,
                label = "Active Tasks",
                current = company.activeTasksCount,
                max = maxTasks,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Storage usage
            val storagePercent = FeatureFlags.getStorageUsagePercentage(company, plan)
            val storageUsedMB = company.storageUsedBytes / (1024 * 1024)
            val storageLimitMB = plan.features.getStorageLimitBytes() / (1024 * 1024)
            
            UsageBar(
                icon = Icons.Default.Storage,
                label = "Storage",
                percentage = storagePercent,
                displayText = "${storageUsedMB}MB / ${storageLimitMB}MB",
                color = when {
                    storagePercent >= 90 -> Color(0xFFD32F2F)
                    storagePercent >= 70 -> Color(0xFFF57C00)
                    else -> MaterialTheme.colorScheme.tertiary
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Photos usage
            val photoPercent = FeatureFlags.getPhotoUsagePercentage(company, plan)
            val maxPhotos = if (plan.features.maxPhotosPerMonth == -1) null else plan.features.maxPhotosPerMonth
            
            if (maxPhotos != null) {
                UsageBar(
                    icon = Icons.Default.PhotoCamera,
                    label = "Photos This Month",
                    percentage = photoPercent,
                    displayText = "${company.photosUploadedThisMonth} / $maxPhotos",
                    color = when {
                        photoPercent >= 90 -> Color(0xFFD32F2F)
                        photoPercent >= 70 -> Color(0xFFF57C00)
                        else -> Color(0xFF7B1FA2)
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Upgrade button if limits are being approached
            if (storagePercent >= 70 || photoPercent >= 70 || 
                (maxGroups != null && company.groupsCount >= maxGroups * 0.8) ||
                (maxTasks != null && company.activeTasksCount >= maxTasks * 0.8)) {
                
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                
                Button(
                    onClick = onUpgradeClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upgrade Plan")
                }
            }
        }
    }
}

/**
 * Simple counter-style usage indicator
 */
@Composable
private fun UsageIndicator(
    icon: ImageVector,
    label: String,
    current: Int,
    max: Int?,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        Text(
            text = if (max != null) "$current / $max" else "$current / ∞",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (max != null && current >= max) {
                Color(0xFFD32F2F)
            } else {
                color
            }
        )
    }
}

/**
 * Progress bar style usage indicator
 */
@Composable
private fun UsageBar(
    icon: ImageVector,
    label: String,
    percentage: Int,
    displayText: String,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Animated progress bar
        val animatedProgress by animateFloatAsState(
            targetValue = (percentage / 100f).coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 1000),
            label = "progress"
        )
        
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )
        
        // Warning text if approaching limit
        if (percentage >= 90) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "⚠️ Approaching limit",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFD32F2F)
            )
        }
    }
}

/**
 * Compact usage summary chip
 */
@Composable
fun UsageSummaryChip(
    company: Company,
    plan: Plan,
    onClick: () -> Unit
) {
    val storagePercent = FeatureFlags.getStorageUsagePercentage(company, plan)
    val photoPercent = FeatureFlags.getPhotoUsagePercentage(company, plan)
    val isNearLimit = storagePercent >= 80 || photoPercent >= 80
    
    FilterChip(
        selected = false,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isNearLimit) Icons.Default.Warning else Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isNearLimit) "Usage Warning" else "${plan.name} Plan"
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = if (isNearLimit) {
                Color(0xFFFFEBEE)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            labelColor = if (isNearLimit) {
                Color(0xFFD32F2F)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    )
}
