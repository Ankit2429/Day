package com.day.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.navArgument
import com.day.app.ui.components.GlobalSearchModal
import com.day.app.ui.components.PremiumGlassBottomNav
import com.day.app.ui.details.TaskDetailsScreen
import com.day.app.ui.details.TaskDetailsViewModel
import com.day.app.ui.documents.DocumentDetailsScreen
import com.day.app.ui.documents.DocumentDetailsViewModel
import com.day.app.ui.documents.DocumentsScreen
import com.day.app.ui.documents.DocumentsViewModel
import com.day.app.ui.documents.PdfViewerScreen
import com.day.app.ui.documents.PdfViewerViewModel
import com.day.app.ui.esp8266.Esp8266Screen
import com.day.app.ui.esp8266.Esp8266ViewModel
import com.day.app.ui.home.HomeScreen
import com.day.app.ui.home.HomeViewModel
import com.day.app.ui.notes.NoteEditorScreen
import com.day.app.ui.notes.NoteEditorViewModel
import com.day.app.ui.notes.NotesScreen
import com.day.app.ui.notes.NotesViewModel
import com.day.app.ui.settings.SettingsScreen
import com.day.app.ui.settings.SettingsViewModel
import com.day.app.ui.taskeditor.TaskEditorScreen
import com.day.app.ui.taskeditor.TaskEditorViewModel
import com.day.app.ui.tasks.TasksScreen
import com.day.app.ui.tasks.TasksViewModel

@Composable
fun DayNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var isSearchModalVisible by rememberSaveable { mutableStateOf(false) }

    val isTopLevelRoute = currentRoute in listOf(
        Screen.Home.route,
        Screen.Tasks.route,
        Screen.Notes.route,
        Screen.Documents.route
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.day.app.ui.theme.DayBackground)
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.97f, animationSpec = tween(220))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.97f, animationSpec = tween(180))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.97f, animationSpec = tween(220))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.97f, animationSpec = tween(180))
            }
        ) {
            composable(Screen.Splash.route) {
                    com.day.app.ui.launch.LaunchScreen(
                        onTimeout = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Splash.route) {
                                    inclusive = true
                                }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    val viewModel: HomeViewModel = viewModel()
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToAddTask = { time -> navController.navigate(Screen.AddTask.createRoute(time)) },
                        onNavigateToAddNote = { navController.navigate(Screen.NoteEditor.createRoute(0L)) },
                        onNavigateToDocuments = { navController.navigate(Screen.Documents.route) },
                        onNavigateToTaskDetails = { id ->
                            navController.navigate(Screen.TaskDetails.createRoute(id))
                        },
                        onNavigateToEsp8266 = { navController.navigate(Screen.Esp8266.route) },
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                    )
                }

                composable(Screen.Tasks.route) {
                    val viewModel: TasksViewModel = viewModel()
                    TasksScreen(
                        viewModel = viewModel,
                        onNavigateToAddTask = { time -> navController.navigate(Screen.AddTask.createRoute(time)) },
                        onNavigateToTaskDetails = { id ->
                            navController.navigate(Screen.TaskDetails.createRoute(id))
                        }
                    )
                }

                composable(Screen.Notes.route) {
                    val viewModel: NotesViewModel = viewModel()
                    NotesScreen(
                        viewModel = viewModel,
                        onNavigateToNoteEditor = { id ->
                            navController.navigate(Screen.NoteEditor.createRoute(id))
                        }
                    )
                }

                composable(Screen.Documents.route) {
                    val viewModel: DocumentsViewModel = viewModel()
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        viewModel.setTab(com.day.app.ui.documents.DocumentTab.FILES)
                    }
                    DocumentsScreen(
                        viewModel = viewModel,
                        onNavigateToDocumentDetails = { id ->
                            navController.navigate(Screen.DocumentDetails.createRoute(id))
                        },
                        onNavigateToPdfViewer = { id ->
                            navController.navigate(Screen.PdfViewer.createRoute(id))
                        },
                        onNavigateToNoteEditor = { id ->
                            navController.navigate(Screen.NoteEditor.createRoute(id))
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    val viewModel: SettingsViewModel = viewModel()
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToEsp8266 = { navController.navigate(Screen.Esp8266.route) }
                    )
                }

                composable(
                    route = Screen.AddTask.route,
                    arguments = listOf(navArgument("time") {
                        type = NavType.LongType
                        defaultValue = -1L
                    })
                ) { backStackEntry ->
                    val viewModel: TaskEditorViewModel = viewModel()
                    val prefillTime = backStackEntry.arguments?.getLong("time") ?: -1L
                    androidx.compose.runtime.LaunchedEffect(prefillTime) {
                        if (prefillTime > 0L) {
                            viewModel.updateScheduledTime(prefillTime)
                        }
                    }
                    TaskEditorScreen(
                        taskId = 0L,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.EditTask.route,
                    arguments = listOf(navArgument("taskId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
                    val viewModel: TaskEditorViewModel = viewModel()
                    TaskEditorScreen(
                        taskId = taskId,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.TaskDetails.route,
                    arguments = listOf(navArgument("taskId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
                    val viewModel: TaskDetailsViewModel = viewModel()
                    TaskDetailsScreen(
                        taskId = taskId,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEdit = { id ->
                            navController.navigate(Screen.EditTask.createRoute(id))
                        }
                    )
                }

                composable(Screen.Esp8266.route) {
                    val viewModel: Esp8266ViewModel = viewModel()
                    Esp8266Screen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.PdfViewer.route,
                    arguments = listOf(navArgument("documentId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val docId = backStackEntry.arguments?.getLong("documentId") ?: 0L
                    val viewModel: PdfViewerViewModel = viewModel()
                    PdfViewerScreen(
                        documentId = docId,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToDetails = { id ->
                            navController.navigate(Screen.DocumentDetails.createRoute(id))
                        }
                    )
                }

                composable(
                    route = Screen.DocumentDetails.route,
                    arguments = listOf(navArgument("documentId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val docId = backStackEntry.arguments?.getLong("documentId") ?: 0L
                    val viewModel: DocumentDetailsViewModel = viewModel()
                    DocumentDetailsScreen(
                        documentId = docId,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPdfViewer = { id ->
                            navController.navigate(Screen.PdfViewer.createRoute(id))
                        }
                    )
                }

                composable(
                    route = Screen.NoteEditor.route,
                    arguments = listOf(navArgument("noteId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
                    val viewModel: NoteEditorViewModel = viewModel()
                    NoteEditorScreen(
                        noteId = noteId,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            if (isTopLevelRoute && currentRoute != null) {
                PremiumGlassBottomNav(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onSearchClick = { isSearchModalVisible = true },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Unified Global Search Modal Sheet
            GlobalSearchModal(
                isVisible = isSearchModalVisible,
                onDismiss = { isSearchModalVisible = false },
                onNavigateToTask = { taskId ->
                    navController.navigate(Screen.TaskDetails.createRoute(taskId))
                },
                onNavigateToNote = { noteId ->
                    navController.navigate(Screen.NoteEditor.createRoute(noteId))
                },
                onNavigateToDocument = { docId ->
                    navController.navigate(Screen.DocumentDetails.createRoute(docId))
                }
            )
        }
    }

