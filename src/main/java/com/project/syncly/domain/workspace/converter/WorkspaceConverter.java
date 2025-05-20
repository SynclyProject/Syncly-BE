package com.project.syncly.domain.workspace.converter;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.workspace.dto.WorkspaceResponseDto;
import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspace.entity.WorkspaceInvitation;
import com.project.syncly.domain.workspace.entity.enums.InvitationType;
import com.project.syncly.domain.workspace.entity.enums.WorkspaceType;
import com.project.syncly.domain.workspaceMember.entity.WorkspaceMember;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.hibernate.jdbc.Work;

import java.time.LocalDateTime;
import java.util.List;

public class WorkspaceConverter {

    public static Workspace toPersonalWorkspace(Long memberId) {
        return Workspace.builder()
                .workspaceName("회원 " + memberId + "의 워크스페이스")
                .workspaceType(WorkspaceType.PERSONAL)
                .build();
    }

    public static Workspace toTeamWorkspace(String workspaceName) {
        return Workspace.builder()
                .workspaceName(workspaceName)
                .workspaceType(WorkspaceType.TEAM)
                .build();
    }

    public static WorkspaceResponseDto.CreateWorkspaceResponseDto toWorkspaceResponse(Workspace workspace) {
        return WorkspaceResponseDto.CreateWorkspaceResponseDto.builder()
                .workspaceId(workspace.getId())
                .workspaceName(workspace.getWorkspaceName())
                .workspaceType(workspace.getWorkspaceType().name())
                .createdAt(workspace.getCreatedAt())
                .build();
    }

    public static WorkspaceInvitation toInvitation(Workspace workspace, Member inviter, Member invitee, String token) {
        return WorkspaceInvitation.builder()
                .workspace(workspace)
                .inviter(inviter)
                .invitee(invitee)
                .token(token)
                .type(InvitationType.PENDING)
                .sentAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    public static WorkspaceResponseDto.InviteWorkspaceResponseDto toInviteResponse(WorkspaceInvitation invitation, String inviteeEmail) {
        return WorkspaceResponseDto.InviteWorkspaceResponseDto.builder()
                .inviteeEmail(inviteeEmail)
                .token(invitation.getToken())
                .expiredAt(invitation.getExpiredAt().toString())
                .build();
    }

    public static WorkspaceResponseDto.AcceptWorkspaceResponseDto toAcceptInviteResponse(WorkspaceInvitation invitation) {
        return WorkspaceResponseDto.AcceptWorkspaceResponseDto.builder()
                .workspaceId(invitation.getWorkspace().getId())
                .workspaceName(invitation.getWorkspace().getWorkspaceName())
                .inviter(invitation.getInviter().getName())
                .invitee(invitation.getInvitee().getName())
                .respondedAt(invitation.getRespondedAt())
                .build();
    }

    public static WorkspaceResponseDto.RejectWorkspaceResponseDto toRejectInviteResponse(WorkspaceInvitation invitation) {
        return WorkspaceResponseDto.RejectWorkspaceResponseDto.builder()
                .invitationId(invitation.getId())
                .respondedAt(invitation.getRespondedAt())
                .build();
    }

    public static List<WorkspaceResponseDto.InvitationInfoDto> toInvitationListResponse(List<WorkspaceInvitation> invitations) {
        return invitations.stream()
                .map(invite -> WorkspaceResponseDto.InvitationInfoDto.builder()
                        .invitationId(invite.getId())
                        .workspaceName(invite.getWorkspace().getWorkspaceName())
                        .inviterName(invite.getInviter().getName())
                        .expiredAt(invite.getExpiredAt().toString())
                        .build()
                ).toList();
    }

    public static WorkspaceResponseDto.RenameWorkspaceResponseDto toRenameWorkspaceResponse(Workspace workspace) {
        return WorkspaceResponseDto.RenameWorkspaceResponseDto.builder()
                .workspaceId(workspace.getId())
                .newName(workspace.getWorkspaceName())
                .build();
    }


    public static WorkspaceResponseDto.LeaveWorkspaceResponseDto leaveWorkspaceResponse(Long workspaceId, Long memberId, String workspaceName, LocalDateTime leavedAt) {
        return WorkspaceResponseDto.LeaveWorkspaceResponseDto.builder()
                .workspaceId(workspaceId)
                .workspaceMemberId(memberId)
                .workspaceName(workspaceName)
                .leavedAt(leavedAt)
                .build();
    }


}
