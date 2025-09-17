package com.project.syncly.domain.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class FileRequestDto {

    @Schema(description = "파일 이름 변경 요청 DTO")
    public record Update(
            @NotNull(message = "파일 이름은 필수입니다.")
            @Pattern(
                    regexp = "^[a-zA-Z0-9가-힣_.-]{1,100}$",
                    message = "파일 이름은 1~100자 사이의 한글, 영문, 숫자, '-', '_', '.'만 사용할 수 있습니다."
            )String name
    ) {}
}