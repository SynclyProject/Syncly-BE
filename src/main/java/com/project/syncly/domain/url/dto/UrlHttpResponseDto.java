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

    @Builder
    @Schema(description = "크롬 익스텐션 탭 저장 응답 DTO")
    public record SaveTabsResponseDto(
            @Schema(description = "생성된 URL 탭 ID", example = "1")
            Long id,

            @Schema(description = "저장된 URL 배열")
            List<String> urls,

            @Schema(description = "생성 시간", example = "2025-09-30T23:45:00")
            LocalDateTime createdAt,

            @Schema(description = "저장된 URL 개수", example = "3")
            Integer count
    ) {}

}
