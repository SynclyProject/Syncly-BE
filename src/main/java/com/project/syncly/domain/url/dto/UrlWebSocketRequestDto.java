package com.project.syncly.domain.url.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public class UrlWebSocketRequestDto {
    @Schema(description = "URL 탭 생성 요청 DTO")
    public record CreateUrlTabRequestDto(
            @Schema(description = "워크스페이스 ID", example = "7")
            @NotNull(message = "워크스페이스 ID는 필수입니다.")
            Long workspaceId,

            @Schema(description = "생성할 URL 탭 이름", example = "졸업프로젝트 관련 URL 모음")
            @NotNull(message = "urlTabName은 필수입니다.")
            String urlTabName
    ) {}

    @Schema(description = "URL 탭 삭제 요청 DTO")
    public record DeleteUrlTabRequestDto(
            @Schema(description = "워크스페이스 ID", example = "7")
            @NotNull(message = "워크스페이스 ID는 필수입니다.")
            Long workspaceId,

            @Schema(description = "삭제할 URL 탭 ID", example = "12")
            @NotNull(message = "urlTabId는 필수입니다.")
            Long urlTabId
    ) {}

    @Schema(description = "URL 탭 이름 변경 요청 DTO")
    public record UpdateUrlTabNameRequestDto(
            @Schema(description = "워크스페이스 ID", example = "7")
            @NotNull(message = "워크스페이스 ID는 필수입니다.")
            Long workspaceId,

            @Schema(description = "URL 탭 ID", example = "12")
            @NotNull(message = "urlTabId는 필수입니다.")
            Long urlTabId,

            @Schema(description = "변경할 URL 탭 이름", example = "새로운 탭 이름")
            @NotNull(message = "변경할 이름은 필수입니다.")
            String newUrlTabName
    ) {}


}




