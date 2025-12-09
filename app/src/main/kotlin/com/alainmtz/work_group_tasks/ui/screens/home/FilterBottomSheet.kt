package com.alainmtz.work_group_tasks.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alainmtz.work_group_tasks.domain.models.TaskPriority
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    onDismiss: () -> Unit,
    currentPriority: TaskPriority?,
    currentDateRange: Pair<Long, Long>?,
    onApply: (TaskPriority?, Pair<Long, Long>?) -> Unit
) {
    var selectedPriority by remember { mutableStateOf(currentPriority) }
    var startDate by remember { mutableStateOf(currentDateRange?.first) }
    var endDate by remember { mutableStateOf(currentDateRange?.second) }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text("Filter Tasks", style = MaterialTheme.typography.headlineSmall)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Priority", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPriority == null,
                    onClick = { selectedPriority = null },
                    label = { Text("All") }
                )
                TaskPriority.values().forEach { p ->
                    FilterChip(
                        selected = selectedPriority == p,
                        onClick = { selectedPriority = p },
                        label = { Text(p.name) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Date Range", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (startDate != null) SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(startDate!!))
                        else "Start Date"
                    )
                }
                
                OutlinedButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (endDate != null) SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(endDate!!))
                        else "End Date"
                    )
                }
            }
            
            if (startDate != null || endDate != null) {
                TextButton(onClick = { 
                    startDate = null
                    endDate = null
                }) {
                    Text("Clear Dates")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    onApply(selectedPriority, if (startDate != null && endDate != null) startDate!! to endDate!! else null)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply Filters")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = datePickerState.selectedDateMillis
                    showStartDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDate = datePickerState.selectedDateMillis
                    showEndDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
