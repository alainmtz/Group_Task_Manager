package com.alainmtz.work_group_tasks.ui.screens.company

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alainmtz.work_group_tasks.domain.models.Company
import com.alainmtz.work_group_tasks.domain.models.CompanyRole
import com.alainmtz.work_group_tasks.domain.models.Plan
import com.alainmtz.work_group_tasks.domain.models.User
import com.alainmtz.work_group_tasks.domain.services.CompanyPlanProvider
import com.alainmtz.work_group_tasks.domain.services.CompanyService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CompanyManagementViewModel : ViewModel() {
    
    private val auth = FirebaseAuth.getInstance()
    
    private val _members = MutableStateFlow<List<User>>(emptyList())
    val members: StateFlow<List<User>> = _members.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    val currentCompany: StateFlow<Company?> = CompanyPlanProvider.currentCompany
    val currentPlan: StateFlow<Plan> = CompanyPlanProvider.currentPlan
    
    val currentUserId: String = auth.currentUser?.uid ?: ""
    
    init {
        loadMembers()
    }
    
    fun loadMembers() {
        val companyId = currentCompany.value?.id ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            
            val result = CompanyService.getCompanyMembers(companyId)
            result.onSuccess { membersList ->
                _members.value = membersList.sortedByDescending { user ->
                    val priority: Int = when (user.role) {
                        CompanyRole.OWNER -> 3
                        CompanyRole.ADMIN -> 2
                        CompanyRole.MEMBER -> 1
                        null -> 0
                    }
                    priority
                }
            }.onFailure { error ->
                Log.e("CompanyVM", "Error loading members", error)
                _errorMessage.value = "Error loading members: ${error.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    fun updateCompanyName(newName: String) {
        val companyId = currentCompany.value?.id ?: return
        
        if (newName.isBlank()) {
            _errorMessage.value = "Company name cannot be empty"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            val result = CompanyService.updateCompanyName(companyId, newName)
            result.onSuccess {
                _successMessage.value = "Company name updated"
            }.onFailure { error ->
                Log.e("CompanyVM", "Error updating name", error)
                _errorMessage.value = "Error: ${error.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    fun addMember(email: String, role: CompanyRole) {
        val companyId = currentCompany.value?.id ?: return
        
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorMessage.value = "Please enter a valid email"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            val result = CompanyService.addMemberToCompany(companyId, email, role)
            result.onSuccess {
                _successMessage.value = "Member added successfully"
                loadMembers() // Reload list
            }.onFailure { error ->
                Log.e("CompanyVM", "Error adding member", error)
                _errorMessage.value = error.message ?: "Error adding member"
            }
            
            _isLoading.value = false
        }
    }
    
    fun removeMember(userId: String, userName: String) {
        val companyId = currentCompany.value?.id ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            
            val result = CompanyService.removeMemberFromCompany(companyId, userId)
            result.onSuccess {
                _successMessage.value = "$userName removed from company"
                loadMembers()
            }.onFailure { error ->
                Log.e("CompanyVM", "Error removing member", error)
                _errorMessage.value = error.message ?: "Error removing member"
            }
            
            _isLoading.value = false
        }
    }
    
    fun updateMemberRole(userId: String, userName: String, newRole: CompanyRole) {
        val companyId = currentCompany.value?.id ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            
            val result = CompanyService.updateMemberRole(companyId, userId, newRole)
            result.onSuccess {
                _successMessage.value = "$userName is now ${newRole.name}"
                loadMembers()
            }.onFailure { error ->
                Log.e("CompanyVM", "Error updating role", error)
                _errorMessage.value = error.message ?: "Error updating role"
            }
            
            _isLoading.value = false
        }
    }
    
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
