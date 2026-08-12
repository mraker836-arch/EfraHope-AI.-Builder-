package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.Navbar
import com.example.ui.screens.*
import com.example.ui.theme.EfraHopeTheme
import com.example.data.models.toProjectModel
import com.example.viewmodel.AppScreen
import com.example.viewmodel.ProjectViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ProjectViewModel = viewModel()

            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val filteredProjects by viewModel.filteredProjects.collectAsStateWithLifecycle()
            val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
            val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
            val projectFiles by viewModel.currentProjectFiles.collectAsStateWithLifecycle()
            val fileSearchQuery by viewModel.fileSearchQuery.collectAsStateWithLifecycle()
            val fileTree by viewModel.fileTree.collectAsStateWithLifecycle()
            val unsavedFileContents by viewModel.unsavedFileContents.collectAsStateWithLifecycle()
            val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
            val selectedFileId by viewModel.selectedFileId.collectAsStateWithLifecycle()
            val previewDevice by viewModel.previewDevice.collectAsStateWithLifecycle()
            val isFullScreenPreview by viewModel.isFullScreenPreview.collectAsStateWithLifecycle()
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            val aiOperationState by viewModel.aiOperationState.collectAsStateWithLifecycle()
            val activePlan by viewModel.activePlan.collectAsStateWithLifecycle()
            val activeChangePlan by viewModel.activeChangePlan.collectAsStateWithLifecycle()
            val currentErrors by viewModel.currentProjectErrors.collectAsStateWithLifecycle()
            val autoFixEnabled by viewModel.autoFixEnabled.collectAsStateWithLifecycle()
            val projectHealth by viewModel.projectHealth.collectAsStateWithLifecycle()
            val previewState by viewModel.previewState.collectAsStateWithLifecycle()
            val databaseSchema by viewModel.databaseSchema.collectAsStateWithLifecycle()
            val apiContract by viewModel.apiContract.collectAsStateWithLifecycle()
            val databaseHealthStatus by viewModel.databaseHealthStatus.collectAsStateWithLifecycle()
            val environmentConfig by viewModel.environmentConfig.collectAsStateWithLifecycle()
            val schemaChangePlan by viewModel.schemaChangePlan.collectAsStateWithLifecycle()

            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
            val authError by viewModel.authError.collectAsStateWithLifecycle()
            val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

            val projectVersions by viewModel.projectVersions.collectAsStateWithLifecycle()
            val projectMembers by viewModel.projectMembers.collectAsStateWithLifecycle()
            val isRestoring by viewModel.isRestoring.collectAsStateWithLifecycle()

            val releaseHistory by viewModel.releaseHistory.collectAsStateWithLifecycle()
            val activeRelease by viewModel.activeRelease.collectAsStateWithLifecycle()
            val isBuildingRelease by viewModel.isBuildingRelease.collectAsStateWithLifecycle()
            val isDeployingRelease by viewModel.isDeployingRelease.collectAsStateWithLifecycle()

            // State for Create Project pre-filling
            var pendingTemplateName by remember { mutableStateOf("") }
            var pendingTemplateDesc by remember { mutableStateOf("") }
            var pendingTemplateType by remember { mutableStateOf("Rice Trading") }

            EfraHopeTheme(darkTheme = isDarkTheme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Navbar(
                            currentScreen = currentScreen,
                            onNavigate = { viewModel.navigateTo(it) },
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { viewModel.toggleTheme() },
                            currentUser = currentUser
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            AppScreen.LANDING -> LandingScreen(
                                onStartBuilding = {
                                    pendingTemplateName = ""
                                    pendingTemplateDesc = ""
                                    pendingTemplateType = "Rice Trading"
                                    viewModel.navigateTo(AppScreen.CREATE_PROJECT)
                                },
                                onExploreProjects = {
                                    viewModel.navigateTo(AppScreen.DASHBOARD)
                                },
                                onSelectTemplate = { name, desc, type ->
                                    pendingTemplateName = name
                                    pendingTemplateDesc = desc
                                    pendingTemplateType = type
                                    viewModel.navigateTo(AppScreen.CREATE_PROJECT)
                                }
                            )

                            AppScreen.DASHBOARD -> DashboardScreen(
                                projects = filteredProjects,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { query -> viewModel.setSearchQuery(query) },
                                onOpenProject = { id -> viewModel.openProject(id) },
                                onCreateNewProject = {
                                    pendingTemplateName = ""
                                    pendingTemplateDesc = ""
                                    pendingTemplateType = "Rice Trading"
                                    viewModel.navigateTo(AppScreen.CREATE_PROJECT)
                                },
                                onRenameProject = { id, name, desc ->
                                    viewModel.renameProject(id, name, desc)
                                },
                                onDeleteProject = { id -> viewModel.deleteProject(id) }
                            )

                            AppScreen.CREATE_PROJECT -> CreateProjectScreen(
                                initialName = pendingTemplateName,
                                initialDesc = pendingTemplateDesc,
                                initialType = pendingTemplateType,
                                onCreateProject = { name, desc, type, style ->
                                    viewModel.createNewProject(name, desc, type, style)
                                },
                                onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) }
                            )

                            AppScreen.WORKSPACE -> {
                                val activeProj = currentProject ?: filteredProjects.firstOrNull()
                                val activeModel = activeProj?.toProjectModel()
                                val userRole = viewModel.authorizationService.getProjectRole(currentUser, activeModel)
                                val isReadOnly = !viewModel.authorizationService.canEditProject(currentUser, activeModel)

                                WorkspaceScreen(
                                    project = activeProj,
                                    files = projectFiles,
                                    treeRoot = fileTree,
                                    selectedFileId = selectedFileId,
                                    fileSearchQuery = fileSearchQuery,
                                    unsavedFileContents = unsavedFileContents,
                                    chatMessages = chatMessages,
                                    previewDevice = previewDevice,
                                    isFullScreenPreview = isFullScreenPreview,
                                    aiOperationState = aiOperationState,
                                    activePlan = activePlan,
                                    activeChangePlan = activeChangePlan,
                                    errors = currentErrors,
                                    onFileSearchQueryChange = { q -> viewModel.setFileSearchQuery(q) },
                                    onSelectFile = { id -> viewModel.selectFile(id) },
                                    onToggleFolder = { folderPath -> viewModel.toggleFolderExpanded(folderPath) },
                                    onCreateFolder = { folderPath -> viewModel.createFolder(folderPath) },
                                    onAddFile = { path, content, lang ->
                                        viewModel.addFileToProject(path, content, lang)
                                    },
                                    onRenameFile = { id, newPath ->
                                        viewModel.renameFile(id, newPath)
                                    },
                                    onDeleteFile = { id ->
                                        viewModel.deleteFile(id)
                                    },
                                    onUpdateUnsavedContent = { id, newContent, origContent ->
                                        viewModel.updateUnsavedContent(id, newContent, origContent)
                                    },
                                    onSaveFileContent = { id, content ->
                                        viewModel.saveFileContent(id, content)
                                    },
                                    onSaveAllFiles = {
                                        viewModel.saveAllFiles(projectFiles)
                                    },
                                    onSendChatMessage = { msg ->
                                        viewModel.sendChatMessage(msg)
                                    },
                                    onApprovePlan = {
                                        viewModel.approvePlan()
                                    },
                                    onRegeneratePlan = {
                                        viewModel.regeneratePlan()
                                    },
                                    onApplyChangePlan = {
                                        viewModel.applyActiveChangePlan()
                                    },
                                    onRejectChangePlan = {
                                        viewModel.rejectActiveChangePlan()
                                    },
                                    onCancelChangePlan = {
                                        viewModel.rejectActiveChangePlan()
                                    },
                                    onRollback = {
                                        viewModel.rollbackLastChange()
                                    },
                                    onTogglePreviewDevice = { device ->
                                        viewModel.setPreviewDevice(device)
                                    },
                                    onToggleFullScreenPreview = { fullScreen ->
                                        viewModel.setFullScreenPreview(fullScreen)
                                    },
                                    onResolveError = { errorId ->
                                        viewModel.resolveError(errorId)
                                    },
                                    previewState = previewState,
                                    onRefreshPreview = { viewModel.refreshPreview() },
                                    onStopPreview = { viewModel.stopPreview() },
                                    onSetViewportMode = { mode -> viewModel.setPreviewViewport(mode) },
                                    databaseSchema = databaseSchema,
                                    apiContract = apiContract,
                                    databaseHealthStatus = databaseHealthStatus,
                                    environmentConfig = environmentConfig,
                                    schemaChangePlan = schemaChangePlan,
                                    onApproveSchemaChangePlan = { plan -> viewModel.approveSchemaChangePlan(plan) },
                                    onRejectSchemaChangePlan = { viewModel.rejectSchemaChangePlan() },
                                    userRole = userRole,
                                    isReadOnly = isReadOnly,
                                    projectVersions = projectVersions,
                                    projectMembers = projectMembers,
                                    onCompareVersions = { vA, vB -> viewModel.compareVersions(vA, vB) },
                                    onGetFileHistory = { path -> viewModel.getFileHistory(path) },
                                    onPreviewRollback = { verId -> viewModel.previewRollback(verId) },
                                    onExecuteRollback = { verId -> viewModel.executeRollback(verId) },
                                    onAddMember = { email, role -> viewModel.addProjectMember(email, role) },
                                    onUpdateRole = { mId, role -> viewModel.updateProjectMemberRole(mId, role) },
                                    onRemoveMember = { mId -> viewModel.removeProjectMember(mId) },
                                    isRestoring = isRestoring,
                                    releases = releaseHistory,
                                    activeRelease = activeRelease,
                                    canCreateRelease = viewModel.authorizationService.canCreateRelease(currentUser, activeModel),
                                    canApproveRelease = viewModel.authorizationService.canApproveRelease(currentUser, activeModel),
                                    canDeployRelease = viewModel.authorizationService.canDeployRelease(currentUser, activeModel),
                                    canRollbackRelease = viewModel.authorizationService.canRollbackRelease(currentUser, activeModel),
                                    isBuildingRelease = isBuildingRelease,
                                    isDeployingRelease = isDeployingRelease,
                                    onCreateReleaseCandidate = { v, name, desc, env ->
                                        viewModel.createReleaseCandidate(v, name, desc, env)
                                    },
                                    onValidateRelease = { relId -> viewModel.validateRelease(relId) },
                                    onBuildRelease = { relId -> viewModel.buildRelease(relId) },
                                    onApproveRelease = { relId -> viewModel.approveRelease(relId) },
                                    onDeployRelease = { relId, confirm -> viewModel.deployRelease(relId, confirm) },
                                    onRollbackRelease = { relId, targetId -> viewModel.rollbackRelease(relId, targetId) },
                                    onUpdateReleaseNotes = { relId, notes -> viewModel.updateReleaseNotes(relId, notes) }
                                )
                            }

                            AppScreen.SETTINGS -> SettingsScreen(
                                currentProject = currentProject,
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = { viewModel.toggleTheme() }
                            )

                            AppScreen.LOGIN -> AuthScreen(
                                initialMode = AuthMode.LOGIN,
                                authError = authError,
                                onSignIn = { email, pass -> viewModel.signIn(email, pass) },
                                onSignUp = { email, pass, name -> viewModel.signUp(email, pass, name) },
                                onResetPassword = { email -> viewModel.resetPassword(email) },
                                onClearError = { viewModel.clearAuthError() }
                            )

                            AppScreen.SIGNUP -> AuthScreen(
                                initialMode = AuthMode.SIGNUP,
                                authError = authError,
                                onSignIn = { email, pass -> viewModel.signIn(email, pass) },
                                onSignUp = { email, pass, name -> viewModel.signUp(email, pass, name) },
                                onResetPassword = { email -> viewModel.resetPassword(email) },
                                onClearError = { viewModel.clearAuthError() }
                            )

                            AppScreen.FORGOT_PASSWORD -> AuthScreen(
                                initialMode = AuthMode.FORGOT_PASSWORD,
                                authError = authError,
                                onSignIn = { email, pass -> viewModel.signIn(email, pass) },
                                onSignUp = { email, pass, name -> viewModel.signUp(email, pass, name) },
                                onResetPassword = { email -> viewModel.resetPassword(email) },
                                onClearError = { viewModel.clearAuthError() }
                            )

                            AppScreen.PROFILE -> ProfileScreen(
                                user = currentUser,
                                session = currentSession,
                                auditLogs = auditLogs,
                                onUpdateProfile = { name, avatar -> viewModel.updateProfile(name, avatar) },
                                onSignOut = { viewModel.signOut() }
                            )
                        }
                    }
                }
            }
        }
    }
}
