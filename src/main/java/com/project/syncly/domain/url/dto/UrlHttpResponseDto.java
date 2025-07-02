package com.project.syncly.domain.url.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;


public class UrlHttpResponseDto {
    @Builder
    @Schema(description = "워크스페이스 전체 탭 및 각 탭에 속한 URL 아이템 리스트 응답 DTO")
    public record TabsWithUrlsResponseDto(
            @Schema(description = "워크스페이스 ID", example = "7")
            Long workspaceId,

            @Schema(description = "워크스페이스 내 탭 리스트")
            List<TabWithUrls> tabs
    ) {
    }

    @Builder
    @Schema(description = "탭 정보 + 해당 탭의 URL 리스트")
    public record TabWithUrls(
            @Schema(description = "URL 탭 ID", example = "12")
            Long tabId,

            @Schema(description = "탭 이름", example = "졸업 프로젝트 관련")
            String tabName,

            @Schema(description = "탭 생성일시", example = "2025-07-02T15:00:00")
            LocalDateTime createdAt,

            @Schema(description = "해당 탭에 포함된 URL 리스트")
            List<UrlItemInfo> urls
    ) {
    }

    @Builder
    @Schema(description = "URL 아이템 정보")
    public record UrlItemInfo(
            @Schema(description = "URL 아이템 ID", example = "101")
            Long urlItemId,

            @Schema(description = "URL 문자열", example = "https://github.com")
            String url,

            @Schema(description = "생성일시", example = "2025-07-02T15:01:00")
            LocalDateTime createdAt
    ) {
    }
}
