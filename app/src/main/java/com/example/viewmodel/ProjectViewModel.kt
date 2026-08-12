package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.models.AIActivityLog
import com.example.data.ai.models.PlannerResult
import com.example.data.ai.operation.AIOperationManager
import com.example.data.ai.orchestrator.MasterAIOrchestrator
import com.example.data.db.AppDatabase
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectFileEntity
import com.example.data.models.AIOperationState
import com.example.data.models.AppError
import com.example.data.models.ErrorSeverity
import com.example.data.models.FixProposal
import com.example.data.models.ProjectHealth
import com.example.data.models.ValidationHistoryRecord
import com.example.data.models.TreeNode
import com.example.data.models.toProjectModel
import com.example.data.repository.ProjectRepository
import com.example.data.storage.RoomStorageProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.example.data.preview.PreviewService
import com.example.data.preview.PreviewState
import com.example.data.preview.ViewportMode
import com.example.data.db.schema.DatabaseSchema
import com.example.data.db.schema.SchemaChangePlan
import com.example.data.db.schema.DatabaseHealthStatus
import com.example.data.api.APIRouteContract
import com.example.data.api.EnvironmentConfig

import com.example.data.auth.models.*
import com.example.data.version.models.*
import com.example.data.version.service.*
import com.example.data.release.models.*
import com.example.data.release.service.*

enum class AppScreen {
    LANDING,
    DASHBOARD,
    CREATE_PROJECT,
    WORKSPACE,
    SETTINGS,
    LOGIN,
    SIGNUP,
    FORGOT_PASSWORD,
    PROFILE
}

enum class PreviewDevice {
    DESKTOP,
    TABLET,
    MOBILE
}

class ProjectViewModel(application: Application) : AndroidViewModel(application) {

    val auditLogService = com.example.data.auth.service.AuditLogService()
    val authService = com.example.data.auth.service.AuthService(auditLogService = auditLogService)
    val authorizationService = com.example.data.auth.service.AuthorizationService()

    val versionControlService = VersionControlService(auditLogService = auditLogService)
    val rollbackService = RollbackService(
        versionControlService = versionControlService,
        authorizationService = authorizationService,
        auditLogService = auditLogService
    )
    val conflictDetectionService = ConflictDetectionService(
        versionControlService = versionControlService,
        auditLogService = auditLogService
    )
    val projectMemberService = ProjectMemberService(
        authorizationService = authorizationService,
        auditLogService = auditLogService
    )

    val gitHubService = GitHubService()
    val gitHubDeploymentProvider = GitHubDeploymentProvider(gitHubService = gitHubService)

    val releaseEngine = ReleaseEngine(
        authService = authorizationService,
        auditLogService = auditLogService,
        versionControlService = versionControlService,
        rollbackService = rollbackService
    )

    val releaseHistory: StateFlow<List<Release>> = releaseEngine.releaseHistory
    val activeRelease: StateFlow<Release?> = releaseEngine.activeRelease

    private val _gitHubConfig = MutableStateFlow(GitHubConfig(owner = "efrahope-org", repo = "efrahope-ai-app", branch = "main"))
    val gitHubConfig: StateFlow<GitHubConfig> = _gitHubConfig.asStateFlow()

    private val _gitHubConnectionStatus = MutableStateFlow(GitHubConnectionStatus.NOT_CONNECTED)
    val gitHubConnectionStatus: StateFlow<GitHubConnectionStatus> = _gitHubConnectionStatus.asStateFlow()

    private val _gitHubValidationResult = MutableStateFlow<GitHubValidationResult?>(null)
    val gitHubValidationResult: StateFlow<GitHubValidationResult?> = _gitHubValidationResult.asStateFlow()

    private val _gitHubStatusMessage = MutableStateFlow("")
    val gitHubStatusMessage: StateFlow<String> = _gitHubStatusMessage.asStateFlow()

    private val _isBuildingRelease = MutableStateFlow(false)
    val isBuildingRelease: StateFlow<Boolean> = _isBuildingRelease.asStateFlow()

    private val _isDeployingRelease = MutableStateFlow(false)
    val isDeployingRelease: StateFlow<Boolean> = _isDeployingRelease.asStateFlow()

    val authState: StateFlow<com.example.data.auth.models.AuthState> = authService.authState
    val currentUser: StateFlow<com.example.data.auth.models.User?> = authService.currentUser
    val currentSession: StateFlow<com.example.data.auth.models.AuthSession?> = authService.currentSession
    val authError: StateFlow<String?> = authService.authError
    val auditLogs: StateFlow<List<com.example.data.auth.models.AuditEvent>> = auditLogService.logs

    val projectVersions: StateFlow<List<ProjectVersion>> = versionControlService.versions
    val projectMembers: StateFlow<List<ProjectMember>> = projectMemberService.members

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _rollbackError = MutableStateFlow<String?>(null)
    val rollbackError: StateFlow<String?> = _rollbackError.asStateFlow()

    private val repository: ProjectRepository
    val orchestrator = MasterAIOrchestrator()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _fileSearchQuery = MutableStateFlow("")
    val fileSearchQuery: StateFlow<String> = _fileSearchQuery.asStateFlow()

    val aiOperationState: StateFlow<AIOperationState> = AIOperationManager.currentState
    val aiActivityLogs: StateFlow<List<AIActivityLog>> = AIOperationManager.activityLogs

    private val _activePlan = MutableStateFlow<PlannerResult?>(null)
    val activePlan: StateFlow<PlannerResult?> = _activePlan.asStateFlow()

    private val _databaseSchema = MutableStateFlow<DatabaseSchema?>(null)
    val databaseSchema: StateFlow<DatabaseSchema?> = _databaseSchema.asStateFlow()

    private val _apiContract = MutableStateFlow<APIRouteContract?>(null)
    val apiContract: StateFlow<APIRouteContract?> = _apiContract.asStateFlow()

    private val _databaseHealthStatus = MutableStateFlow(DatabaseHealthStatus.DEVELOPMENT)
    val databaseHealthStatus: StateFlow<DatabaseHealthStatus> = _databaseHealthStatus.asStateFlow()

    private val _schemaChangePlan = MutableStateFlow<SchemaChangePlan?>(null)
    val schemaChangePlan: StateFlow<SchemaChangePlan?> = _schemaChangePlan.asStateFlow()

    private val _environmentConfig = MutableStateFlow(EnvironmentConfig())
    val environmentConfig: StateFlow<EnvironmentConfig> = _environmentConfig.asStateFlow()

    val mockDatabaseProvider = com.example.data.db.abstraction.MockDatabaseProvider()

    val previewService = PreviewService(
        validationEngine = orchestrator.validationEngine,
        onErrorReported = { err -> addError(err.message, err.severity, err.source, err.file, err.line) }
    )
    val previewState: StateFlow<PreviewState> = previewService.previewState

    private val _activeChangePlan = MutableStateFlow<com.example.data.ai.models.ChangePlan?>(null)
    val activeChangePlan: StateFlow<com.example.data.ai.models.ChangePlan?> = _activeChangePlan.asStateFlow()

    private val _unsavedFileContents = MutableStateFlow<Map<String, String>>(emptyMap())
    val unsavedFileContents: StateFlow<Map<String, String>> = _unsavedFileContents.asStateFlow()

    private val _expandedFolders = MutableStateFlow<Set<String>>(setOf("", "src", "src/components", "src/pages", "src/features", "src/services", "src/hooks", "src/types"))
    val expandedFolders: StateFlow<Set<String>> = _expandedFolders.asStateFlow()

    private val _customFolders = MutableStateFlow<List<String>>(listOf("src", "src/components", "src/pages", "src/features", "src/services", "src/hooks", "src/types", "configuration"))
    val customFolders: StateFlow<List<String>> = _customFolders.asStateFlow()

    init {
        val dao = AppDatabase.getDatabase(application).projectDao()
        val storageProvider = RoomStorageProvider(dao)
        repository = ProjectRepository(storageProvider)

        // Ensure default Rice Trading Project if database is empty on launch
        viewModelScope.launch {
            repository.allProjects.firstOrNull()?.let { list ->
                if (list.isEmpty()) {
                    repository.createProject(
                        name = "EfraHope Rice Trading App",
                        description = "Online rice trading platform with inventory management, bulk orders, customers and executive sales dashboard.",
                        appType = "Rice Trading",
                        style = "Modern Cyberpunk"
                    )
                }
            }
        }
    }

    val projects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredProjects: StateFlow<List<ProjectEntity>> = combine(projects, searchQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.appType.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentScreen = MutableStateFlow(AppScreen.LANDING)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    val selectedProjectId: StateFlow<String?> = _selectedProjectId.asStateFlow()

    val currentProject: StateFlow<ProjectEntity?> = _selectedProjectId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getProject(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentProjectFiles: StateFlow<List<ProjectFileEntity>> = _selectedProjectId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getProjectFiles(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredProjectFiles: StateFlow<List<ProjectFileEntity>> = combine(currentProjectFiles, fileSearchQuery) { files, query ->
        repository.fileManagementService.searchFiles(files, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fileTree: StateFlow<TreeNode.Folder> = combine(
        currentProject,
        currentProjectFiles,
        customFolders,
        expandedFolders,
        unsavedFileContents
    ) { proj, files, folders, expanded, unsavedMap ->
        val name = proj?.name ?: "Project Root"
        repository.fileManagementService.buildTreeStructure(
            projectName = name,
            files = files,
            customFolders = folders,
            expandedFolders = expanded,
            unsavedFileIds = unsavedMap.keys
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TreeNode.Folder("Project Root", ""))

    val chatMessages: StateFlow<List<ChatMessageEntity>> = _selectedProjectId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getChatMessages(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentProjectErrors: StateFlow<List<AppError>> = _selectedProjectId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getProjectErrors(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFileId = MutableStateFlow<String?>(null)
    val selectedFileId: StateFlow<String?> = _selectedFileId.asStateFlow()

    private val _previewDevice = MutableStateFlow(PreviewDevice.DESKTOP)
    val previewDevice: StateFlow<PreviewDevice> = _previewDevice.asStateFlow()

    private val _isFullScreenPreview = MutableStateFlow(false)
    val isFullScreenPreview: StateFlow<Boolean> = _isFullScreenPreview.asStateFlow()

    private val _activeWorkspaceTab = MutableStateFlow(0) // 0: Preview, 1: Code Editor
    val activeWorkspaceTab: StateFlow<Int> = _activeWorkspaceTab.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            val res = authService.signIn(email, pass)
            if (res.isSuccess) {
                _currentScreen.value = AppScreen.DASHBOARD
            }
        }
    }

    fun signUp(email: String, pass: String, displayName: String) {
        viewModelScope.launch {
            val res = authService.signUp(email, pass, displayName)
            if (res.isSuccess) {
                _currentScreen.value = AppScreen.DASHBOARD
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authService.signOut()
            _selectedProjectId.value = null
            _unsavedFileContents.value = emptyMap()
            _activePlan.value = null
            _activeChangePlan.value = null
            _schemaChangePlan.value = null
            _currentScreen.value = AppScreen.LOGIN
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            authService.resetPassword(email)
        }
    }

    fun updateProfile(displayName: String, avatarUrl: String?) {
        viewModelScope.launch {
            authService.updateProfile(displayName, avatarUrl)
        }
    }

    fun clearAuthError() {
        authService.clearError()
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFileSearchQuery(query: String) {
        _fileSearchQuery.value = query
    }

    fun openProject(projectId: String) {
        viewModelScope.launch {
            val targetProj = repository.getProject(projectId).firstOrNull()
            val user = currentUser.value
            val targetModel = targetProj?.toProjectModel()
            if (targetModel != null && !authorizationService.canReadProject(user, targetModel)) {
                auditLogService.logEvent(
                    userId = user?.id ?: "unauthenticated",
                    projectId = projectId,
                    action = com.example.data.auth.models.AuditAction.PROJECT_OPENED,
                    result = "DENIED",
                    details = "Access denied for user ${user?.email}"
                )
                addError("Access Denied: You do not have permission to view or open this project.")
                return@launch
            }

            _selectedProjectId.value = projectId
            _unsavedFileContents.value = emptyMap()
            _currentScreen.value = AppScreen.WORKSPACE

            ensureInitialVersionCreated(projectId)

            user?.id?.let { uid ->
                auditLogService.logEvent(
                    userId = uid,
                    projectId = projectId,
                    action = com.example.data.auth.models.AuditAction.PROJECT_OPENED,
                    result = "SUCCESS",
                    details = "Opened project ${targetProj?.name}"
                )
            }
        }
    }

    fun selectFile(fileId: String) {
        _selectedFileId.value = fileId
    }

    fun toggleFolderExpanded(folderPath: String) {
        val current = _expandedFolders.value.toMutableSet()
        if (current.contains(folderPath)) {
            current.remove(folderPath)
        } else {
            current.add(folderPath)
        }
        _expandedFolders.value = current
    }

    fun createFolder(folderPath: String) {
        val current = _customFolders.value.toMutableList()
        val clean = folderPath.trim().trim('/')
        if (clean.isNotBlank() && !current.contains(clean)) {
            current.add(clean)
            _customFolders.value = current
            val expanded = _expandedFolders.value.toMutableSet()
            expanded.add(clean)
            _expandedFolders.value = expanded
        }
    }

    fun updateUnsavedContent(fileId: String, content: String, originalContent: String) {
        val map = _unsavedFileContents.value.toMutableMap()
        if (content == originalContent) {
            map.remove(fileId)
        } else {
            map[fileId] = content
        }
        _unsavedFileContents.value = map
    }

    fun createNewProject(name: String, description: String, appType: String, style: String) {
        viewModelScope.launch {
            val user = currentUser.value
            val ownerId = user?.id ?: "dev-user-1"
            AIOperationManager.startOperation("CREATE_PROJECT", "temp", name, AIOperationState.PLANNING, "Initializing project structure...")
            val newId = repository.createProject(name, description, appType, style, ownerId = ownerId)
            AIOperationManager.completeOperation(name, success = true)

            auditLogService.logEvent(
                userId = ownerId,
                projectId = newId,
                action = com.example.data.auth.models.AuditAction.PROJECT_CREATED,
                result = "SUCCESS",
                details = "Created project '$name' with owner $ownerId"
            )

            openProject(newId)
        }
    }

    fun renameProject(projectId: String, newName: String, newDescription: String) {
        viewModelScope.launch {
            val proj = currentProject.value?.toProjectModel()
            val user = currentUser.value
            if (proj != null && !authorizationService.canEditProject(user, proj)) {
                addError("Access Denied: Write permission required to rename project.")
                return@launch
            }
            repository.renameProject(projectId, newName, newDescription)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            val targetProj = repository.getProject(projectId).firstOrNull()
            val user = currentUser.value
            val targetModel = targetProj?.toProjectModel()
            if (targetModel != null && !authorizationService.canDeleteProject(user, targetModel)) {
                auditLogService.logEvent(
                    userId = user?.id ?: "unauthenticated",
                    projectId = projectId,
                    action = com.example.data.auth.models.AuditAction.PROJECT_DELETED,
                    result = "DENIED",
                    details = "Delete project denied for user ${user?.email}"
                )
                addError("Access Denied: Only project owners or platform admins can delete this project.")
                return@launch
            }

            repository.deleteProject(projectId)
            user?.id?.let { uid ->
                auditLogService.logEvent(
                    userId = uid,
                    projectId = projectId,
                    action = com.example.data.auth.models.AuditAction.PROJECT_DELETED,
                    result = "SUCCESS",
                    details = "Deleted project $projectId"
                )
            }

            if (_selectedProjectId.value == projectId) {
                _selectedProjectId.value = null
                _currentScreen.value = AppScreen.DASHBOARD
            }
        }
    }

    fun saveFileContent(fileId: String, content: String) {
        viewModelScope.launch {
            val user = currentUser.value
            val projModel = currentProject.value?.toProjectModel()
            if (projModel != null && !authorizationService.canEditProject(user, projModel)) {
                addError("Access Denied: Write permission required for file modifications (VIEWER mode active).")
                return@launch
            }

            val result = repository.updateFileContent(fileId, content)
            if (result.isSuccess) {
                val map = _unsavedFileContents.value.toMutableMap()
                map.remove(fileId)
                _unsavedFileContents.value = map

                user?.id?.let { uid ->
                    auditLogService.logEvent(
                        userId = uid,
                        projectId = projModel?.id,
                        action = com.example.data.auth.models.AuditAction.PROJECT_MODIFIED,
                        result = "SUCCESS",
                        details = "Saved file content for file $fileId"
                    )
                }
            } else {
                addError(result.exceptionOrNull()?.message ?: "Failed to save file.")
            }
        }
    }

    fun saveAllFiles(files: List<ProjectFileEntity>) {
        viewModelScope.launch {
            val user = currentUser.value
            val projModel = currentProject.value?.toProjectModel()
            if (projModel != null && !authorizationService.canEditProject(user, projModel)) {
                addError("Access Denied: Write permission required for file modifications.")
                return@launch
            }

            val unsaved = _unsavedFileContents.value
            unsaved.forEach { (fileId, content) ->
                repository.updateFileContent(fileId, content)
            }
            _unsavedFileContents.value = emptyMap()
        }
    }

    fun addFileToProject(filePath: String, content: String, language: String? = null) {
        val projId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            val user = currentUser.value
            val projModel = currentProject.value?.toProjectModel()
            if (projModel != null && !authorizationService.canEditProject(user, projModel)) {
                addError("Access Denied: Write permission required to add new files.")
                return@launch
            }

            val result = repository.createFile(projId, filePath, content, language)
            if (result.isSuccess) {
                val file = result.getOrThrow()
                selectFile(file.fileId)
            } else {
                addError(result.exceptionOrNull()?.message ?: "Invalid file path.")
            }
        }
    }

    fun renameFile(fileId: String, newPath: String) {
        viewModelScope.launch {
            val user = currentUser.value
            val projModel = currentProject.value?.toProjectModel()
            if (projModel != null && !authorizationService.canEditProject(user, projModel)) {
                addError("Access Denied: Write permission required to rename files.")
                return@launch
            }

            val result = repository.renameFile(fileId, newPath)
            if (result.isFailure) {
                addError(result.exceptionOrNull()?.message ?: "Rename failed.")
            }
        }
    }

    fun deleteFile(fileId: String) {
        viewModelScope.launch {
            val user = currentUser.value
            val projModel = currentProject.value?.toProjectModel()
            if (projModel != null && !authorizationService.canEditProject(user, projModel)) {
                addError("Access Denied: Write permission required to delete files.")
                return@launch
            }

            val result = repository.deleteFile(fileId)
            if (result.isSuccess) {
                if (_selectedFileId.value == fileId) {
                    _selectedFileId.value = null
                }
                val map = _unsavedFileContents.value.toMutableMap()
                map.remove(fileId)
                _unsavedFileContents.value = map
            } else {
                addError(result.exceptionOrNull()?.message ?: "Delete failed.")
            }
        }
    }

    fun sendChatMessage(userText: String) {
        val projId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            // Save user message to database
            repository.processUserChatMessage(projId, userText) { }

            // Delegate request to Master AI Orchestrator
            val currentProj = currentProject.value
            val currentFiles = currentProjectFiles.value
            val activeFile = selectedFileId.value

            val aiResult = orchestrator.processRequest(userText, currentProj, currentFiles, activeFile)

            if (aiResult.data is Pair<*, *> && (aiResult.data as? Pair<*, *>)?.first is DatabaseSchema) {
                @Suppress("UNCHECKED_CAST")
                val pair = aiResult.data as Pair<DatabaseSchema, APIRouteContract>
                val schema = pair.first
                val contract = pair.second

                _databaseSchema.value = schema
                _apiContract.value = contract
                mockDatabaseProvider.seedFromSchema(schema)

                repository.saveChatMessage(
                    projectId = projId,
                    sender = "ai",
                    text = "Generated Database Schema & API Contract for '${schema.projectName}'. View structured tables and endpoints in the Database tab.",
                    agentName = "Database Planner"
                )
            } else if (aiResult.data is SchemaChangePlan) {
                val plan = aiResult.data as SchemaChangePlan
                _schemaChangePlan.value = plan
                repository.saveChatMessage(
                    projectId = projId,
                    sender = "ai",
                    text = "Generated Schema Change Plan [${plan.planId}]: ${plan.description}. Please review and approve in the Database panel.",
                    agentName = "Database Planner"
                )
            } else if (aiResult.data is PlannerResult) {
                _activePlan.value = aiResult.data
                repository.saveChatMessage(
                    projectId = projId,
                    sender = "ai",
                    text = "Project Plan synthesized for '${aiResult.data.projectName}'. View the structured plan in the AI Panel.",
                    agentName = "Project Planner"
                )
            } else if (aiResult.data is com.example.data.ai.models.ChangePlan) {
                _activeChangePlan.value = aiResult.data
                repository.saveChatMessage(
                    projectId = projId,
                    sender = "ai",
                    text = "Generated Change Plan [${aiResult.data.operationId}]: ${aiResult.data.summary}. Please review and approve the changes.",
                    agentName = "Code Generator"
                )
            } else if (aiResult.success) {
                val textResponse = aiResult.data?.toString() ?: aiResult.message
                repository.saveChatMessage(
                    projectId = projId,
                    sender = "ai",
                    text = textResponse,
                    agentName = "Master AI"
                )
            } else {
                repository.saveChatMessage(
                    projectId = projId,
                    sender = "ai",
                    text = "AI Processing Issue: ${aiResult.message}",
                    agentName = "System"
                )
            }
        }
    }

    fun applyActiveChangePlan() {
        val plan = _activeChangePlan.value ?: return
        val projId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            val activeVer = versionControlService.getActiveVersion(projId)
            val activeVerNum = activeVer?.versionNumber ?: 1

            // Validate stale AI operation
            val conflictResult = conflictDetectionService.validateAIOperation(projId, activeVerNum, plan.affectedFiles)
            if (conflictResult.hasConflict) {
                addError(conflictResult.message)
                return@launch
            }

            val success = repository.applyChangePlan(projId, plan)
            if (success) {
                _activeChangePlan.value = null
                captureSnapshotAndVersion(
                    reason = "AI Change Plan Applied: ${plan.summary}",
                    source = VersionSource.AI_CHANGE,
                    label = "AI - ${plan.summary.take(30)}",
                    aiOpId = plan.operationId,
                    aiPrompt = plan.intent
                )
                refreshPreview(isAutoRefresh = true)
            } else {
                addError("Failed to apply Change Plan [${plan.operationId}].")
            }
        }
    }

    fun rejectActiveChangePlan() {
        val plan = _activeChangePlan.value ?: return
        val projId = _selectedProjectId.value ?: return
        _activeChangePlan.value = null
        viewModelScope.launch {
            repository.saveChatMessage(
                projectId = projId,
                sender = "system",
                text = "Change Plan [${plan.operationId}] rejected by user.",
                agentName = "User Review"
            )
        }
    }

    fun rollbackLastChange() {
        val projId = _selectedProjectId.value ?: return
        val latestSnapshot = com.example.data.ai.operation.RollbackManager.getLatestSnapshotForProject(projId)
        if (latestSnapshot == null) {
            addError("No previous snapshot found for rollback.")
            return
        }
        viewModelScope.launch {
            val success = repository.rollbackChange(projId, latestSnapshot.snapshotId)
            if (!success) {
                addError("Failed to rollback snapshot [${latestSnapshot.snapshotId}].")
            }
        }
    }

    fun approvePlan() {
        val plan = _activePlan.value ?: return
        _activePlan.value = plan.copy(approved = true)
        val projId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            repository.saveChatMessage(
                projectId = projId,
                sender = "ai",
                text = "Project Plan for '${plan.projectName}' approved by user. Prepared for construction.",
                agentName = "Project Planner"
            )
        }
    }

    fun regeneratePlan() {
        val currentProj = currentProject.value ?: return
        sendChatMessage("Regenerate project plan for ${currentProj.name} - ${currentProj.description}")
    }

    private val _autoFixEnabled = MutableStateFlow(false)
    val autoFixEnabled: StateFlow<Boolean> = _autoFixEnabled.asStateFlow()

    private val _projectHealth = MutableStateFlow(ProjectHealth.HEALTHY)
    val projectHealth: StateFlow<ProjectHealth> = _projectHealth.asStateFlow()

    private val _activeFixProposal = MutableStateFlow<FixProposal?>(null)
    val activeFixProposal: StateFlow<FixProposal?> = _activeFixProposal.asStateFlow()

    private val _validationHistory = MutableStateFlow<List<ValidationHistoryRecord>>(emptyList())
    val validationHistory: StateFlow<List<ValidationHistoryRecord>> = _validationHistory.asStateFlow()

    private val validationEngine = com.example.data.ai.validation.ValidationEngine()
    private val errorFixingAgent = com.example.data.ai.agents.ErrorFixingAgent(validationEngine)
    private val fixWorkflowManager = com.example.data.ai.operation.FixWorkflowManager(errorFixingAgent, validationEngine)

    fun toggleAutoFix(enabled: Boolean) {
        _autoFixEnabled.value = enabled
        fixWorkflowManager.autoFixEnabled = enabled
    }

    fun runProjectValidation() {
        val projId = _selectedProjectId.value ?: return
        val projEntity = currentProject.value ?: return
        val files = currentProjectFiles.value

        viewModelScope.launch {
            AIOperationManager.startOperation("VALIDATING_PROJECT", projId, projEntity.name, AIOperationState.VALIDATING, "Running full static analysis and validation engine...")
            val result = validationEngine.validateProject(projEntity, files)
            _projectHealth.value = result.health

            // Save detected errors to database
            repository.clearErrors(projId)
            result.errors.forEach { err ->
                repository.saveError(projId, err)
            }
            result.warnings.forEach { warn ->
                repository.saveError(projId, warn)
            }

            val record = ValidationHistoryRecord(
                projectId = projId,
                validationType = "FULL",
                isSuccess = result.isValid,
                errorsCount = result.errors.size,
                warningsCount = result.warnings.size,
                durationMs = result.durationMs,
                triggeredBy = "User"
            )

            val currentHistory = _validationHistory.value.toMutableList()
            currentHistory.add(record)
            _validationHistory.value = currentHistory

            AIOperationManager.completeOperation(projEntity.name, success = result.isValid)

            repository.saveChatMessage(
                projectId = projId,
                sender = "system",
                text = "Project Validation Complete (${result.durationMs}ms):\n• Health: **${result.health.name}**\n• Errors: **${result.errors.size}**\n• Warnings: **${result.warnings.size}**",
                agentName = "Validation Engine"
            )
        }
    }

    fun analyzeAndProposeFix(error: AppError) {
        val projId = _selectedProjectId.value ?: return
        val projEntity = currentProject.value ?: return
        val files = currentProjectFiles.value

        viewModelScope.launch {
            AIOperationManager.startOperation("ANALYZING_ERROR", projId, projEntity.name, AIOperationState.ANALYZING, "Analyzing root cause for error '${error.id}'...")
            val proposal = errorFixingAgent.analyzeAndProposeFix(error, projEntity, files)
            _activeFixProposal.value = proposal
            _activeChangePlan.value = proposal.changePlan

            AIOperationManager.completeOperation(projEntity.name, success = true)

            repository.saveChatMessage(
                projectId = projId,
                sender = "ai",
                text = "🔍 **Error Analysis & Fix Proposal**:\n• **Root Cause**: ${proposal.rootCause}\n• **Confidence**: `${proposal.confidence.name}` (${proposal.confidenceReason})\n• **Risk Level**: ${proposal.risk}\n• **Explanation**: ${proposal.explanation}\n\nReview the proposed ChangePlan to apply.",
                agentName = "Error Fixing Agent"
            )
        }
    }

    fun applyFixProposal(proposal: FixProposal) {
        val projId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            val success = repository.applyChangePlan(projId, proposal.changePlan)
            if (success) {
                repository.resolveError(proposal.errorId)
                _activeFixProposal.value = null
                _activeChangePlan.value = null
                runProjectValidation()
            } else {
                addError("Failed to apply Fix Proposal for error '${proposal.errorId}'.")
            }
        }
    }

    fun addError(
        message: String,
        severity: ErrorSeverity = ErrorSeverity.ERROR,
        source: String = "System",
        file: String? = null,
        line: Int? = null
    ) {
        val projId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            val error = AppError(
                message = message,
                severity = severity,
                source = source,
                file = file,
                line = line
            )
            repository.saveError(projId, error)
        }
    }

    fun resolveError(errorId: String) {
        viewModelScope.launch {
            repository.resolveError(errorId)
        }
    }

    fun clearErrors() {
        val projId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            repository.clearErrors(projId)
        }
    }

    fun startPreview() {
        val proj = currentProject.value ?: return
        val fileList = currentProjectFiles.value
        previewService.startPreview(proj, fileList)
    }

    fun refreshPreview(isAutoRefresh: Boolean = false) {
        val proj = currentProject.value ?: return
        val fileList = currentProjectFiles.value
        previewService.refreshPreview(proj, fileList, isAutoRefresh)
    }

    fun stopPreview() {
        previewService.stopPreview()
    }

    fun setPreviewViewport(mode: ViewportMode) {
        previewService.setViewportMode(mode)
        when (mode) {
            ViewportMode.DESKTOP -> _previewDevice.value = PreviewDevice.DESKTOP
            ViewportMode.TABLET -> _previewDevice.value = PreviewDevice.TABLET
            ViewportMode.MOBILE -> _previewDevice.value = PreviewDevice.MOBILE
        }
    }

    fun setPreviewDevice(device: PreviewDevice) {
        _previewDevice.value = device
        when (device) {
            PreviewDevice.DESKTOP -> previewService.setViewportMode(ViewportMode.DESKTOP)
            PreviewDevice.TABLET -> previewService.setViewportMode(ViewportMode.TABLET)
            PreviewDevice.MOBILE -> previewService.setViewportMode(ViewportMode.MOBILE)
        }
    }

    fun setFullScreenPreview(fullScreen: Boolean) {
        _isFullScreenPreview.value = fullScreen
    }

    fun approveSchemaChangePlan(plan: SchemaChangePlan) {
        val currentSchema = _databaseSchema.value ?: return
        val updatedSchema = orchestrator.databasePlannerAgent.applyChangePlan(currentSchema, plan)
        _databaseSchema.value = updatedSchema
        _schemaChangePlan.value = null
        mockDatabaseProvider.seedFromSchema(updatedSchema)
        refreshPreview(isAutoRefresh = true)
    }

    fun rejectSchemaChangePlan() {
        _schemaChangePlan.value = null
    }

    fun setWorkspaceTab(tabIndex: Int) {
        _activeWorkspaceTab.value = tabIndex
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    // ==========================================
    // PHASE 8: VERSION CONTROL & COLLABORATION
    // ==========================================

    fun captureSnapshotAndVersion(
        reason: String,
        source: VersionSource = VersionSource.USER_CHANGE,
        label: String = "",
        aiOpId: String? = null,
        aiPrompt: String? = null
    ) {
        val projId = _selectedProjectId.value ?: return
        val user = currentUser.value
        val userId = user?.id ?: "dev-user-1"
        val files = currentProjectFiles.value.map { FileSnapshot(it.filePath, it.fileContent, it.language) }
        val proj = currentProject.value
        val meta = mapOf(
            "name" to (proj?.name ?: ""),
            "description" to (proj?.description ?: ""),
            "appType" to (proj?.appType ?: ""),
            "style" to (proj?.style ?: "")
        )

        val snapshot = versionControlService.createSnapshot(
            projectId = projId,
            createdBy = userId,
            reason = reason,
            files = files,
            projectMetadata = meta,
            schemaVersion = databaseSchema.value?.version ?: 1,
            schemaContent = databaseSchema.value?.entities?.joinToString("\n") { it.name }
        )

        versionControlService.createVersion(
            projectId = projId,
            snapshotId = snapshot.id,
            label = label.ifBlank { reason },
            description = reason,
            createdBy = user?.email ?: userId,
            source = source,
            changedFilesCount = files.size,
            validationPassed = true,
            aiOperationId = aiOpId,
            aiUserPrompt = aiPrompt
        )
    }

    fun ensureInitialVersionCreated(projectId: String) {
        if (versionControlService.getVersionsForProject(projectId).isEmpty()) {
            val files = currentProjectFiles.value.map { FileSnapshot(it.filePath, it.fileContent, it.language) }
            val proj = currentProject.value
            val meta = mapOf(
                "name" to (proj?.name ?: "Initial Project"),
                "description" to (proj?.description ?: ""),
                "appType" to (proj?.appType ?: "App"),
                "style" to (proj?.style ?: "Default")
            )

            val snapshot = versionControlService.createSnapshot(
                projectId = projectId,
                createdBy = currentUser.value?.id ?: "system",
                reason = "Initial project creation baseline state",
                files = files,
                projectMetadata = meta
            )

            versionControlService.createVersion(
                projectId = projectId,
                snapshotId = snapshot.id,
                label = "v1 - Project Initialized",
                description = "Initial baseline project creation",
                createdBy = currentUser.value?.email ?: "system",
                source = VersionSource.SYSTEM,
                changedFilesCount = files.size
            )
        }
    }

    fun compareVersions(verAId: String, verBId: String): VersionComparisonResult? {
        return versionControlService.compareVersions(verAId, verBId)
    }

    fun getFileHistory(filePath: String): List<FileVersionRecord> {
        val projId = _selectedProjectId.value ?: return emptyList()
        return versionControlService.getFileHistory(projId, filePath)
    }

    fun previewRollback(targetVersionId: String): RollbackPreview? {
        val projId = _selectedProjectId.value ?: return null
        return rollbackService.previewRollback(projId, targetVersionId)
    }

    fun executeRollback(targetVersionId: String) {
        val projId = _selectedProjectId.value ?: return
        val user = currentUser.value
        val projModel = currentProject.value?.toProjectModel()

        viewModelScope.launch {
            _isRestoring.value = true
            _rollbackError.value = null

            val result = rollbackService.executeRollback(
                projectId = projId,
                targetVersionId = targetVersionId,
                user = user,
                projectModel = projModel,
                repository = repository
            )

            _isRestoring.value = false
            if (result.success) {
                refreshPreview(isAutoRefresh = true)
            } else {
                _rollbackError.value = result.errorMessage
                addError(result.errorMessage ?: "Rollback failed.")
            }
        }
    }

    fun addProjectMember(email: String, role: ProjectRole) {
        val projId = _selectedProjectId.value ?: return
        val user = currentUser.value
        val projModel = currentProject.value?.toProjectModel()

        val res = projectMemberService.addMember(projId, email, role, user, projModel)
        if (res.isFailure) {
            addError(res.exceptionOrNull()?.message ?: "Failed to add member.")
        }
    }

    fun updateProjectMemberRole(memberId: String, newRole: ProjectRole) {
        val projId = _selectedProjectId.value ?: return
        val user = currentUser.value
        val projModel = currentProject.value?.toProjectModel()

        val res = projectMemberService.updateMemberRole(projId, memberId, newRole, user, projModel)
        if (res.isFailure) {
            addError(res.exceptionOrNull()?.message ?: "Failed to update member role.")
        }
    }

    fun removeProjectMember(memberId: String) {
        val projId = _selectedProjectId.value ?: return
        val user = currentUser.value
        val projModel = currentProject.value?.toProjectModel()

        val res = projectMemberService.removeMember(projId, memberId, user, projModel)
        if (res.isFailure) {
            addError(res.exceptionOrNull()?.message ?: "Failed to remove member.")
        }
    }

    // ==========================================
    // PHASE 9: RELEASE ENGINEERING & DEPLOYMENT
    // ==========================================

    fun createReleaseCandidate(
        version: String,
        name: String,
        description: String,
        environment: ReleaseEnvironment
    ) {
        val projId = _selectedProjectId.value ?: return
        val user = currentUser.value
        val projModel = currentProject.value?.toProjectModel()
        val files = currentProjectFiles.value
        val schema = databaseSchema.value

        viewModelScope.launch {
            val result = releaseEngine.createReleaseCandidate(
                projectId = projId,
                version = version,
                name = name,
                description = description,
                environment = environment,
                user = user,
                project = projModel,
                files = files,
                databaseSchema = schema
            )
            if (result.isFailure) {
                addError(result.exceptionOrNull()?.message ?: "Failed to create release candidate.")
            }
        }
    }

    fun validateRelease(releaseId: String) {
        val files = currentProjectFiles.value
        val schema = databaseSchema.value
        val user = currentUser.value
        val projModel = currentProject.value?.toProjectModel()

        viewModelScope.launch {
            releaseEngine.validateRelease(
                releaseId = releaseId,
                files = files,
                databaseSchema = schema,
                user = user,
                project = projModel
            )
        }
    }

    fun buildRelease(releaseId: String) {
        val files = currentProjectFiles.value
        val user = currentUser.value

        viewModelScope.launch {
            _isBuildingRelease.value = true
            releaseEngine.buildRelease(
                releaseId = releaseId,
                files = files,
                user = user
            )
            _isBuildingRelease.value = false
        }
    }

    fun approveRelease(releaseId: String) {
        val user = currentUser.value
        val projModel = currentProject.value?.toProjectModel()

        viewModelScope.launch {
            val res = releaseEngine.approveRelease(releaseId, user, projModel)
            if (res.isFailure) {
                addError(res.exceptionOrNull()?.message ?: "Failed to approve release.")
            }
        }
    }

    fun deployRelease(releaseId: String, userConfirmedInUI: Boolean) {
        val user = currentUser.value
        val projModel = currentProject.value?.toProjectModel()

        viewModelScope.launch {
            _isDeployingRelease.value = true
            val res = releaseEngine.deployRelease(releaseId, user, projModel, userConfirmedInUI)
            _isDeployingRelease.value = false
            if (res.isFailure) {
                addError(res.exceptionOrNull()?.message ?: "Deployment failed.")
            }
        }
    }

    fun rollbackRelease(releaseId: String, targetPreviousReleaseId: String) {
        val user = currentUser.value
        val projModel = currentProject.value?.toProjectModel()

        viewModelScope.launch {
            val res = releaseEngine.rollbackRelease(
                releaseId = releaseId,
                targetPreviousReleaseId = targetPreviousReleaseId,
                user = user,
                project = projModel,
                repository = repository
            )
            if (res.isFailure) {
                addError(res.exceptionOrNull()?.message ?: "Release rollback failed.")
            } else {
                refreshPreview(isAutoRefresh = true)
            }
        }
    }

    fun updateReleaseNotes(releaseId: String, notes: ReleaseNotes) {
        viewModelScope.launch {
            releaseEngine.updateReleaseNotes(releaseId, notes)
        }
    }

    fun updateGitHubConfig(owner: String, repo: String, branch: String, token: String? = null, customDomain: String? = null) {
        val newCfg = GitHubConfig(owner = owner, repo = repo, branch = branch, token = token, customDomain = customDomain)
        _gitHubConfig.value = newCfg
        gitHubDeploymentProvider.config = newCfg
    }

    fun testGitHubConnection() {
        val cfg = _gitHubConfig.value
        val (status, msg) = gitHubDeploymentProvider.connect(cfg)
        _gitHubConnectionStatus.value = status
        _gitHubStatusMessage.value = msg
        if (status == GitHubConnectionStatus.CONNECTED) {
            releaseEngine.deploymentService.setProvider(gitHubDeploymentProvider)
        }
    }

    fun checkGitHubDeployment() {
        val cfg = _gitHubConfig.value
        val files = currentProjectFiles.value
        val valResult = gitHubDeploymentProvider.validateRepository(files)
        _gitHubValidationResult.value = valResult
    }

    fun useGitHubProvider() {
        releaseEngine.deploymentService.setProvider(gitHubDeploymentProvider)
    }

    fun useSimulationProvider() {
        releaseEngine.deploymentService.setProvider(DeploymentSimulationProvider())
    }
}
