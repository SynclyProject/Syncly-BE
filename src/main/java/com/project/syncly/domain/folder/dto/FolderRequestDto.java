package com.project.syncly.domain.folder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class FolderRequestDto {

    @Schema(description = "폴더 생성 요청 DTO")
    public record Create(
            @NotNull(message = "워크스페이스 ID는 필수입니다.")
            Long workspaceId,
            Long parentId,
            @NotNull(message = "폴더 이름은 필수입니다.")
            @Pattern(
                    regexp = "^[a-zA-Z0-9가-힣_-]{1,50}$",
                    message = "폴더 이름은 1~50자 사이의 한글, 영문, 숫자, '-', '_'만 사용할 수 있으며 공백과 특수문자는 사용할 수 없습니다."
            )String name
    ) {}

    @Schema(description = "폴더 이름 변경 요청 DTO")
    public record Update(
            @NotNull(message = "폴더 이름은 필수입니다.")
            @Pattern(
                    regexp = "^[a-zA-Z0-9가-힣_-]{1,50}$",
                    message = "폴더 이름은 1~50자 사이의 한글, 영문, 숫자, '-', '_'만 사용할 수 있으며 공백과 특수문자는 사용할 수 없습니다."
            )String name
    ) {}
}

