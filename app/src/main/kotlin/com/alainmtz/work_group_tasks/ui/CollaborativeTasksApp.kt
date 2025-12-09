package com.alainmtz.work_group_tasks.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alainmtz.work_group_tasks.ui.screens.auth.LoginScreen
import com.alainmtz.work_group_tasks.ui.screens.auth.RegisterScreen
import com.alainmtz.work_group_tasks.ui.screens.chat.ChatDetailScreen
import com.alainmtz.work_group_tasks.ui.screens.chat.NewChatScreen
import com.alainmtz.work_group_tasks.ui.screens.groups.CreateGroupScreen
import com.alainmtz.work_group_tasks.ui.screens.groups.GroupDetailScreen
import com.alainmtz.work_group_tasks.ui.screens.home.HomeScreen
import com.alainmtz.work_group_tasks.ui.screens.profile.ImageCropScreen
import com.alainmtz.work_group_tasks.ui.screens.profile.ProfileScreen
import com.alainmtz.work_group_tasks.ui.screens.tasks.CreateTaskScreen
import com.alainmtz.work_group_tasks.ui.screens.tasks.TaskDetailScreen
import com.alainmtz.work_group_tasks.ui.settings.Theme
import com.alainmtz.work_group_tasks.ui.theme.CollaborativeTasksTheme
import com.alainmtz.work_group_tasks.ui.viewmodels.AuthViewModel
import com.alainmtz.work_group_tasks.ui.viewmodels.ThemeViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun CollaborativeTasksApp(
    themeViewModel: ThemeViewModel = viewModel(),
    intent: Intent? = null
) {
    val theme by themeViewModel.theme.collectAsState()
    val useDarkTheme = when (theme) {
        Theme.LIGHT -> false
        Theme.DARK -> true
        Theme.SYSTEM -> isSystemInDarkTheme()
    }

    CollaborativeTasksTheme(darkTheme = useDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val context = LocalContext.current
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    // Handle permission result if needed
                }
            )

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val navController = rememberNavController()
            val authViewModel: AuthViewModel = viewModel()
            val currentUser by authViewModel.currentUser.collectAsState()

            LaunchedEffect(intent, currentUser) {
                if (currentUser != null) {
                    intent?.let {
                        val threadId = it.getStringExtra("threadId")
                        val groupId = it.getStringExtra("groupId")
                        val taskId = it.getStringExtra("taskId")
                        
                        if (threadId != null) {
                            navController.navigate("chat_detail/$threadId")
                        } else if (groupId != null) {
                            navController.navigate("group_detail/$groupId")
                        } else if (taskId != null) {
                            navController.navigate("task_detail/$taskId")
                        }
                    }
                }
            }

            val startDestination = if (currentUser != null) "home" else "login"

            NavHost(navController = navController, startDestination = startDestination) {
                composable("login") {
                    LoginScreen(
                        onNavigateToRegister = { navController.navigate("register") },
                        onLoginSuccess = {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        viewModel = authViewModel
                    )
                }
                composable("register") {
                    RegisterScreen(
                        onNavigateToLogin = { navController.popBackStack() },
                        onRegisterSuccess = {
                            navController.navigate("home") {
                                popUpTo("register") { inclusive = true }
                            }
                        },
                        viewModel = authViewModel
                    )
                }
                composable("home") {
                    HomeScreen(
                        onNavigateToTaskDetail = { taskId -> navController.navigate("task_detail/$taskId") },
                        onNavigateToCreateTask = { navController.navigate("create_task") },
                        onNavigateToGroupDetail = { groupId -> navController.navigate("group_detail/$groupId") },
                        onNavigateToCreateGroup = { navController.navigate("create_group") },
                        onNavigateToProfile = { navController.navigate("profile") },
                        onNavigateToChatDetail = { threadId -> navController.navigate("chat_detail/$threadId") },
                        onNavigateToNewChat = { navController.navigate("new_chat") }
                    )
                }
                composable("profile") {
                    ProfileScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onSignOut = {
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        onNavigateToImageCrop = { uri ->
                            val encodedUri = URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.toString())
                            navController.navigate("image_crop/$encodedUri")
                        },
                        authViewModel = authViewModel,
                        themeViewModel = themeViewModel
                    )
                }
                composable("image_crop/{imageUri}") { backStackEntry ->
                    val imageUri = backStackEntry.arguments?.getString("imageUri")
                    if (imageUri != null) {
                        ImageCropScreen(
                            imageUri = imageUri,
                            onCropDone = { croppedUri ->
                                authViewModel.updateProfilePicture(croppedUri)
                                navController.popBackStack()
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }
                }
                composable("create_task?groupId={groupId}") { backStackEntry ->
                    val groupId = backStackEntry.arguments?.getString("groupId")
                    CreateTaskScreen(
                        onNavigateBack = { navController.popBackStack() },
                        groupId = groupId
                    )
                }
                composable("task_detail/{taskId}") { backStackEntry ->
                    val taskId = backStackEntry.arguments?.getString("taskId")
                    if (taskId != null) {
                        TaskDetailScreen(
                            taskId = taskId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable("create_group") {
                    CreateGroupScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("group_detail/{groupId}") { backStackEntry ->
                    val groupId = backStackEntry.arguments?.getString("groupId")
                    if (groupId != null) {
                        GroupDetailScreen(
                            groupId = groupId,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToTaskDetail = { taskId -> navController.navigate("task_detail/$taskId") },
                            onNavigateToCreateTask = { gId -> navController.navigate("create_task?groupId=$gId") },
                            onNavigateToChatDetail = { threadId -> navController.navigate("chat_detail/$threadId") }
                        )
                    }
                }
                composable("chat_detail/{threadId}") { backStackEntry ->
                    val threadId = backStackEntry.arguments?.getString("threadId")
                    if (threadId != null) {
                        ChatDetailScreen(
                            threadId = threadId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable("new_chat") {
                    NewChatScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onChatCreated = { threadId ->
                            navController.navigate("chat_detail/$threadId") {
                                popUpTo("new_chat") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
