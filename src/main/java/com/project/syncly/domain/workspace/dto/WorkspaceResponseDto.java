package com.project.syncly.domain.workspace.dto;

import com.project.syncly.domain.workspace.entity.enums.WorkspaceType;
import com.project.syncly.domain.workspaceMember.entity.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;


public class WorkspaceResponseDto {
    @Builder
    @Schema(description = "팀 워크스페이스 생성 응답 DTO")
    public record CreateWorkspaceResponseDto(
            Long workspaceId,
            String workspaceName,
            String workspaceType,
            LocalDateTime createdAt
    ) {
    }

    @Builder
    @Schema(description = "워크스페이스 초대완료 DTO")
    public record InviteWorkspaceResponseDto(
            String inviteeEmail,
            String token,
            String expiredAt
    ) {}


    @Builder
    @Schema(description = "팀 워크스페이스 초대 수락 응답 DTO")
    public record AcceptWorkspaceResponseDto(
            Long workspaceId,
            String workspaceName,
            String inviter,
            String invitee,
            LocalDateTime respondedAt
    ) {
    }

    @Builder
    @Schema(description = "팀 워크스페이스 초대 거절 응답 DTO")
    public record RejectWorkspaceResponseDto(
            Long invitationId,
            LocalDateTime respondedAt
    ) {
    }

    @Builder
    @Schema(description = "팀 워크스페이스 초대 내역 조회 DTO")
    public record InvitationInfoDto(
            Long invitationId,
            String workspaceName,
            String inviterName,
            String expiredAt
    ) {}

    @Builder
    @Schema(description = "팀 워크스페이스 이름 변경 응답 DTO")
    public record RenameWorkspaceResponseDto(
            Long workspaceId,
            String newName
    ) {}

    @Builder
    @Schema(description = "팀 워크스페이스 탈퇴 응답 DTO")
    public record LeaveWorkspaceResponseDto(
            Long workspaceId,
            Long workspaceMemberId,
            String workspaceName,
            LocalDateTime leavedAt
    ) {}

    @Builder
    @Schema(description = "팀 워크스페이스 회원 추방 응답 DTO")
    public record KickMemberResponseDto(
            Long workspaceId,
            Long targetMemberId,
            String workspaceName,
            LocalDateTime deletedAt
    ) {}

    @Builder
    @Schema(description = "워크스페이스 조회 응답 DTO")
    public record MyWorkspaceResponseDto(
            Long workspaceId,
            String workspaceName,
            WorkspaceType workspaceType,
            LocalDateTime createdAt
    ) {}

    @Builder
    @Schema(description = "워크스페이스 삭제 응답 DTO")
    public record DeleteWorkspaceResponseDto(
            Long workspaceId,
            String workspaceName,
            LocalDateTime createdAt,
            LocalDateTime deletedAt
    ) {}




}
