package com.project.syncly.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public class ChatWebSocketRequestDto {
    @Builder
    @Schema(description = "채팅 WebSocket 전송 요청 DTO")
    public record CreateChatRequestDto(
            @Schema(description = "클라이언트에서 생성한 메시지 고유 ID(UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
            String msgId,

            @Schema(description = "메시지 본문", example = "안녕하세요!")
            String content
    ) {
    }
}
