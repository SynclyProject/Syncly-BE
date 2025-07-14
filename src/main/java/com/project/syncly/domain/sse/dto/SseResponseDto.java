package com.project.syncly.domain.sse.dto;

import com.project.syncly.domain.workspace.entity.enums.WorkspaceType;
import com.project.syncly.domain.workspaceMember.entity.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;


public class SseResponseDto {
    @Builder
    @Schema(description = "워크스페이스 초대 알림 응답 DTO")
    public record InvitedNotificationResponseDto(
            @Schema(description = "알림 타입", example = "INVITATION")
            String eventType,
            @Schema(description = "워크스페이스 아이디", example = "12")
            Long workspaceId,
            @Schema(description = "워크스페이스 이름", example = "우리팀 프로젝트")
            String workspaceName,
            @Schema(description = "초대한 시간", example = "2025-07-08T15:32:00")
            LocalDateTime invitedAt,
            @Schema(description = "이 이벤트가 수신된 시간", example = "2025-07-08T15:35:10")
            LocalDateTime receivedAt
    ) {}
}
