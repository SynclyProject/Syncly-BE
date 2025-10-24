package com.project.syncly.domain.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NoteRequestDto {

    @Schema(description = "노트 생성 요청 DTO")
    public record Create(
            @NotBlank(message = "노트 제목은 필수입니다.")
            @Size(max = 200, message = "노트 제목은 최대 200자까지 입력 가능합니다.")
            String title
    ) {}
}
