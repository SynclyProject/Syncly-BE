package com.project.syncly.domain.url.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UrlHttpRequestDto {
    @Schema(description = "URL 탭 생성 요청 DTO")
    public record CreateUrlTabRequestDto(
            @Schema(description = "생성할 URL 탭 이름", example = "졸업프로젝트 관련 URL 모음")
            @NotNull(message = "urlTabName은 필수입니다.")
            String urlTabName
    ) {}


    @Schema(description = "URL 탭 이름 변경 요청 DTO")
    public record UpdateUrlTabNameRequestDto(
            @Schema(description = "변경할 URL 탭 이름", example = "새로운 탭 이름")
            @NotNull(message = "변경할 이름은 필수입니다.")
            String newUrlTabName
    ) {}

    @Schema(description = "URL 아이템 추가 요청 DTO")
    public record AddUrlItemRequestDto(
            @Schema(description = "추가할 URL", example = "https://example.com")
            @NotBlank(message = "URL은 필수입니다.")
            String url
    ) {}

}




