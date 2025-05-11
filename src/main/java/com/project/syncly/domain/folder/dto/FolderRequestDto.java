package com.project.syncly.domain.folder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class FolderRequestDto {

    @Schema(description = "폴더 생성 요청 DTO")
    public record Create(
            @NotNull Long workspaceId,
            Long parentId,
            @NotNull(message = "폴더 이름은 필수입니다.")
            @Pattern(
                    regexp = "^[^\\s]+$",
                    message = "폴더 이름에는 공백이 포함될 수 없습니다."
            )String name
    ) {}
}

