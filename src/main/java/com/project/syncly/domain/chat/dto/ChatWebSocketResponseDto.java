package com.project.syncly.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

public class ChatWebSocketResponseDto {
    @Builder
    @Schema(description = "채팅 WebSocket 응답 DTO")
    public record ChatResponseDto(
            @Schema(description = "메시지 ID", example = "12345")
            Long id,

            @Schema(description = "워크스페이스 ID", example = "77")
            Long workspaceId,

            @Schema(description = "보낸 멤버 ID", example = "1001")
            Long senderId,

            @Schema(description = "보낸 멤버 이름", example = "강바다")
            String senderName,

            @Schema(description = "멤버 프로필", example = "강바다")
            String senderProfileImage,

            @Schema(description = "클라이언트에서 보낸 msgId(UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
            String msgId,

            @Schema(description = "워크스페이스 내 순차 증가 seq", example = "101")
            Long seq,

            @Schema(description = "메시지 본문", example = "안녕하세요!")
            String content,

            @Schema(description = "작성 시간")
            LocalDateTime createdAt
    ) {
    }

}
