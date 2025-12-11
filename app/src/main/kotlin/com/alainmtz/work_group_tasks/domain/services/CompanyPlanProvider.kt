package com.alainmtz.work_group_tasks.domain.services

import android.util.Log
import com.alainmtz.work_group_tasks.domain.models.Company
import com.alainmtz.work_group_tasks.domain.models.Plan
import com.alainmtz.work_group_tasks.domain.models.PlanDefaults
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Singleton provider for current company and plan information.
 * Automatically loads and updates company/plan when user signs in.
 */
object CompanyPlanProvider {
    private const val TAG = "CompanyPlanProvider"

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _currentCompany = MutableStateFlow<Company?>(null)
    val currentCompany: StateFlow<Company?> = _currentCompany.asStateFlow()

    private val _currentPlan = MutableStateFlow<Plan>(PlanDefaults.FREE)
    val currentPlan: StateFlow<Plan> = _currentPlan.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var companyListener: ListenerRegistration? = null
    private var planListener: ListenerRegistration? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    /**
     * Initialize the provider - sets up auth listener to load company/plan on sign-in
     */
    fun initialize() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                loadCompanyAndPlan()
            } else {
                clearData()
            }
        }
        auth.addAuthStateListener(authStateListener!!)
        // Load immediately if already signed in
        if (auth.currentUser != null) {
            loadCompanyAndPlan()
        }
    }

    /**
     * Load company and plan data for current user with real-time listeners
     */
    private fun loadCompanyAndPlan() {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        // First, get user document to find their companyId
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                val companyId = userDoc.getString("companyId")

                if (companyId != null) {
                    // Listen to company updates
                    companyListener?.remove()
                    companyListener = db.collection("companies")
                        .document(companyId)
                        .addSnapshotListener { companyDoc, error ->
                            if (error != null) {
                                Log.e(TAG, "Error listening to company", error)
                                _isLoading.value = false
                                return@addSnapshotListener
                            }

                            if (companyDoc != null && companyDoc.exists()) {
                                val company = Company.fromFirestore(companyDoc)
                                _currentCompany.value = company

                                // Load the plan for this company
                                loadPlan(company.planId)
                            } else {
                                Log.w(TAG, "Company document not found for ID: $companyId")
                                _currentCompany.value = null
                                _currentPlan.value = PlanDefaults.FREE
                                _isLoading.value = false
                            }
                        }
                } else {
                    // No company assigned - use FREE plan
                    Log.i(TAG, "User has no company assigned, using FREE plan")
                    _currentCompany.value = null
                    _currentPlan.value = PlanDefaults.FREE
                    _isLoading.value = false
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching user document", e)
                _currentCompany.value = null
                _currentPlan.value = PlanDefaults.FREE
                _isLoading.value = false
            }
    }

    /**
     * Load plan document and set up real-time listener
     */
    private fun loadPlan(planId: String) {
        planListener?.remove()
        planListener = db.collection("plans")
            .document(planId)
            .addSnapshotListener { planDoc, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to plan", error)
                    _currentPlan.value = PlanDefaults.FREE
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (planDoc != null && planDoc.exists()) {
                    _currentPlan.value = Plan.fromFirestore(planDoc)
                } else {
                    Log.w(TAG, "Plan document not found for ID: $planId, using FREE")
                    _currentPlan.value = PlanDefaults.FREE
                }
                _isLoading.value = false
            }
    }

    /**
     * Clear data on sign-out
     */
    private fun clearData() {
        companyListener?.remove()
        planListener?.remove()
        _currentCompany.value = null
        _currentPlan.value = PlanDefaults.FREE
        _isLoading.value = false
    }

    /**
     * Cleanup listeners (call in Application.onTerminate if needed)
     */
    fun cleanup() {
        authStateListener?.let { auth.removeAuthStateListener(it) }
        companyListener?.remove()
        planListener?.remove()
    }

    /**
     * Force reload company and plan (useful after plan changes)
     */
    suspend fun reload() {
        val userId = auth.currentUser?.uid ?: return

        try {
            val userDoc = db.collection("users").document(userId).get().await()
            val companyId = userDoc.getString("companyId") ?: return

            val companyDoc = db.collection("companies").document(companyId).get().await()
            if (companyDoc.exists()) {
                val company = Company.fromFirestore(companyDoc)
                _currentCompany.value = company

                val planDoc = db.collection("plans").document(company.planId).get().await()
                if (planDoc.exists()) {
                    _currentPlan.value = Plan.fromFirestore(planDoc)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reloading company and plan", e)
        }
    }
}
