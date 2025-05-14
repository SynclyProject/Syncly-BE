package com.project.syncly.domain.workspace.dto;

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


}
