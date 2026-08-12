package com.example.data.auth.service

import com.example.data.auth.models.*
import com.example.data.models.ProjectModel

class AuthorizationService {

    /**
     * Resolves the user's role for a target project.
     * 1. If project ownerId matches user.id -> OWNER.
     * 2. If project members list contains matching member entry -> member.role.
     * 3. Default fallback if user has ADMIN platform role -> OWNER.
     * 4. Otherwise -> VIEWER.
     */
    fun getProjectRole(
        user: User?,
        project: ProjectModel?,
        members: List<ProjectMember> = emptyList()
    ): ProjectRole {
        if (user == null || project == null) return ProjectRole.VIEWER
        if (user.role == UserRole.ADMIN) return ProjectRole.OWNER
        if (project.ownerId == user.id) return ProjectRole.OWNER

        // Check explicit member records
        val allMembers = (project.members + members).distinctBy { it.userId }
        val memberEntry = allMembers.find { it.userId == user.id }
        if (memberEntry != null) {
            return memberEntry.role
        }

        // Unrelated logged-in user viewing default public workspace
        return ProjectRole.VIEWER
    }

    /**
     * Get permission set for a given role.
     */
    fun getRolePermissions(role: ProjectRole): Set<ProjectPermission> {
        return when (role) {
            ProjectRole.OWNER -> ProjectPermission.entries.toSet()
            ProjectRole.EDITOR -> setOf(
                ProjectPermission.PROJECT_READ,
                ProjectPermission.PROJECT_WRITE,
                ProjectPermission.PROJECT_BUILD,
                ProjectPermission.PROJECT_AI,
                ProjectPermission.PROJECT_DATABASE,
                ProjectPermission.PROJECT_HISTORY,
                ProjectPermission.RELEASE_READ,
                ProjectPermission.RELEASE_CREATE
            )
            ProjectRole.VIEWER -> setOf(
                ProjectPermission.PROJECT_READ,
                ProjectPermission.PROJECT_HISTORY,
                ProjectPermission.RELEASE_READ
            )
        }
    }

    /**
     * Check if a user has a specific permission on a project.
     */
    fun hasPermission(
        user: User?,
        project: ProjectModel?,
        permission: ProjectPermission,
        members: List<ProjectMember> = emptyList()
    ): Boolean {
        if (user == null || project == null) return false
        val role = getProjectRole(user, project, members)
        val permissions = getRolePermissions(role)
        return permissions.contains(permission)
    }

    fun canReadProject(user: User?, project: ProjectModel?): Boolean {
        return hasPermission(user, project, ProjectPermission.PROJECT_READ)
    }

    fun canEditProject(user: User?, project: ProjectModel?): Boolean {
        return hasPermission(user, project, ProjectPermission.PROJECT_WRITE)
    }

    fun canDeleteProject(user: User?, project: ProjectModel?): Boolean {
        return hasPermission(user, project, ProjectPermission.PROJECT_DELETE)
    }

    fun canShareProject(user: User?, project: ProjectModel?): Boolean {
        return hasPermission(user, project, ProjectPermission.PROJECT_SHARE)
    }

    fun canUseAI(user: User?, project: ProjectModel?): Boolean {
        return hasPermission(user, project, ProjectPermission.PROJECT_AI)
    }

    fun canManageDatabase(user: User?, project: ProjectModel?): Boolean {
        return hasPermission(user, project, ProjectPermission.PROJECT_DATABASE)
    }

    fun canBuildProject(user: User?, project: ProjectModel?): Boolean {
        return hasPermission(user, project, ProjectPermission.PROJECT_BUILD)
    }

    fun canViewHistory(user: User?, project: ProjectModel?): Boolean {
        return hasPermission(user, project, ProjectPermission.PROJECT_HISTORY)
    }

    fun canRestoreProject(user: User?, project: ProjectModel?): Boolean {
        return hasPermission(user, project, ProjectPermission.PROJECT_RESTORE)
    }

    fun canReadRelease(user: User?, project: ProjectModel?): Boolean {
        return hasPermission(user, project, ProjectPermission.RELEASE_READ)
    }

    fun canCreateRelease(user: User?, project: ProjectModel?): Boolean {
        return hasPermission(user, project, ProjectPermission.RELEASE_CREATE)
    }

    fun canApproveRelease(user: User?, project: ProjectModel?): Boolean {
        return hasPermission(user, project, ProjectPermission.RELEASE_APPROVE)
    }

    fun canDeployRelease(user: User?, project: ProjectModel?): Boolean {
        return hasPermission(user, project, ProjectPermission.RELEASE_DEPLOY)
    }

    fun canRollbackRelease(user: User?, project: ProjectModel?): Boolean {
        return hasPermission(user, project, ProjectPermission.RELEASE_ROLLBACK)
    }
}
