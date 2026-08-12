package com.example.data.version.service

import com.example.data.auth.models.*
import com.example.data.auth.service.AuditLogService
import com.example.data.auth.service.AuthorizationService
import com.example.data.models.ProjectModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ProjectMemberService(
    private val authorizationService: AuthorizationService = AuthorizationService(),
    private val auditLogService: AuditLogService = AuditLogService()
) {

    private val _members = MutableStateFlow<List<ProjectMember>>(emptyList())
    val members: StateFlow<List<ProjectMember>> = _members.asStateFlow()

    fun getMembersForProject(projectId: String, projectModel: ProjectModel?): List<ProjectMember> {
        val storedMembers = _members.value.filter { it.projectId == projectId }
        val modelMembers = projectModel?.members ?: emptyList()

        val ownerEntry = if (projectModel != null && !storedMembers.any { it.userId == projectModel.ownerId && it.role == ProjectRole.OWNER }) {
            listOf(
                ProjectMember(
                    id = "owner-${projectModel.ownerId}",
                    projectId = projectId,
                    userId = projectModel.ownerId,
                    userEmail = "owner@efrahope.ai",
                    role = ProjectRole.OWNER,
                    status = MemberStatus.ACTIVE,
                    invitedBy = "system"
                )
            )
        } else emptyList()

        return (ownerEntry + modelMembers + storedMembers).distinctBy { it.id }
    }

    fun addMember(
        projectId: String,
        email: String,
        role: ProjectRole,
        actorUser: User?,
        projectModel: ProjectModel?
    ): Result<ProjectMember> {
        if (!authorizationService.hasPermission(actorUser, projectModel, ProjectPermission.PROJECT_SHARE)) {
            auditLogService.logEvent(
                userId = actorUser?.id ?: "unauthenticated",
                projectId = projectId,
                action = AuditAction.MEMBER_ADDED,
                result = "DENIED",
                details = "User ${actorUser?.email} lacks PROJECT_SHARE permission"
            )
            return Result.failure(IllegalAccessException("Access Denied: Only Project Owner can add members."))
        }

        val newMember = ProjectMember(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            userId = "user-${UUID.randomUUID().toString().take(6)}",
            userEmail = email,
            role = role,
            status = MemberStatus.ACTIVE,
            invitedBy = actorUser?.id ?: "system",
            createdAt = System.currentTimeMillis()
        )

        val updated = _members.value.toMutableList()
        updated.add(newMember)
        _members.value = updated

        auditLogService.logEvent(
            userId = actorUser?.id ?: "system",
            projectId = projectId,
            action = AuditAction.MEMBER_ADDED,
            result = "SUCCESS",
            details = "Added member $email with role ${role.name}"
        )

        return Result.success(newMember)
    }

    fun updateMemberRole(
        projectId: String,
        memberId: String,
        newRole: ProjectRole,
        actorUser: User?,
        projectModel: ProjectModel?
    ): Result<ProjectMember> {
        if (!authorizationService.hasPermission(actorUser, projectModel, ProjectPermission.PROJECT_SHARE)) {
            auditLogService.logEvent(
                userId = actorUser?.id ?: "unauthenticated",
                projectId = projectId,
                action = AuditAction.ROLE_CHANGED,
                result = "DENIED",
                details = "User ${actorUser?.email} lacks PROJECT_SHARE permission"
            )
            return Result.failure(IllegalAccessException("Access Denied: Only Project Owner can update member roles."))
        }

        val list = _members.value.toMutableList()
        val index = list.indexOfFirst { it.id == memberId && it.projectId == projectId }
        if (index == -1) {
            return Result.failure(IllegalArgumentException("Member not found"))
        }

        val updatedMember = list[index].copy(role = newRole)
        list[index] = updatedMember
        _members.value = list

        auditLogService.logEvent(
            userId = actorUser?.id ?: "system",
            projectId = projectId,
            action = AuditAction.ROLE_CHANGED,
            result = "SUCCESS",
            details = "Updated member ${updatedMember.userEmail} role to ${newRole.name}"
        )

        return Result.success(updatedMember)
    }

    fun removeMember(
        projectId: String,
        memberId: String,
        actorUser: User?,
        projectModel: ProjectModel?
    ): Result<Boolean> {
        if (!authorizationService.hasPermission(actorUser, projectModel, ProjectPermission.PROJECT_SHARE)) {
            auditLogService.logEvent(
                userId = actorUser?.id ?: "unauthenticated",
                projectId = projectId,
                action = AuditAction.MEMBER_REMOVED,
                result = "DENIED",
                details = "User ${actorUser?.email} lacks PROJECT_SHARE permission"
            )
            return Result.failure(IllegalAccessException("Access Denied: Only Project Owner can remove members."))
        }

        val memberToRemove = _members.value.find { it.id == memberId && it.projectId == projectId }
        _members.value = _members.value.filterNot { it.id == memberId && it.projectId == projectId }

        auditLogService.logEvent(
            userId = actorUser?.id ?: "system",
            projectId = projectId,
            action = AuditAction.MEMBER_REMOVED,
            result = "SUCCESS",
            details = "Removed member ${memberToRemove?.userEmail ?: memberId} from project"
        )

        return Result.success(true)
    }
}
