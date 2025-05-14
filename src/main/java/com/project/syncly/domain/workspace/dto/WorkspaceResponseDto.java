package com.project.syncly.domain.workspace.dto;

import lombok.Builder;

import java.time.LocalDateTime;


public class WorkspaceResponseDto {
    @Builder
    public record CreateWorkspaceResponseDto(
            Long workspaceId,
            String workspaceName,
            String workspaceType,
            LocalDateTime createdAt
    ) {
    }
}
