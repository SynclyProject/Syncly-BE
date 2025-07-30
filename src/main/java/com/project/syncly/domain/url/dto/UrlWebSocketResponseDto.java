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
            String action,
            Long urlTabId,
            Long workspaceId,
            String urlTabName,
            LocalDateTime createdAt
    ) {
    }

    @Builder
    @Schema(description = "URL 탭 삭제 응답 DTO")
    public record DeleteUrlTabResponseDto(
            String action,
            Long urlTabId,
            Long workspaceId,
            LocalDateTime deletedAt
    ) {
    }

    @Builder
    @Schema(description = "URL 탭 이름 변경 응답 DTO")
    public record UpdateUrlTabNameResponseDto(
            String action,
            Long urlTabId,
            Long workspaceId,
            String updatedTabName,
            LocalDateTime updatedAt
    ) {
    }

    @Builder
    @Schema(description = "URL 아이템 추가 응답 DTO")
    public record AddUrlItemResponseDto(
            String action,
            Long urlTabId,
            Long urlItemId,
            String url,
            LocalDateTime createdAt
    ) {}

    @Builder
    @Schema(description = "URL 아이템 삭제 응답 DTO")
    public record DeleteUrlItemResponseDto(
            String action,
            Long urlTabId,
            Long urlItemId,
            LocalDateTime deletedAt
    ) {}




}
