package com.project.syncly.domain.url.dto;

import com.project.syncly.domain.workspace.entity.enums.WorkspaceType;
import com.project.syncly.domain.workspaceMember.entity.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;


public class UrlWebSocketResponseDto {
    @Builder
    @Schema(description = "팀 워크스페이스 생성 응답 DTO")
    public record CreateUrlTabResponseDto(
            Long urlTabId,
            Long workspaceId,
            String urlTabName,
            LocalDateTime createdAt
    ) {
    }
}
