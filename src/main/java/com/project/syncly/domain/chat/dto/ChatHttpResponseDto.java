package com.project.syncly.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public class ChatHttpResponseDto {
    @Builder
    @Schema(description = "HTTP 메시지 페이지 조회 응답 DTO")
    public record ChatResponseDto(
            @Schema(description = "메시지 리스트")
            List<ChatWebSocketResponseDto.ChatResponseDto> items, // 재사용

            @Schema(description = "다음 페이지 조회용 beforeSeq (null이면 더 없음)")
            Long nextBeforeSeq,

            @Schema(description = "서버 기준 최신 seq")
            Long latestSeq
    ) {
    }
}
