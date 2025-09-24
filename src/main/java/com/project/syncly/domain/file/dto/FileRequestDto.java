package com.project.syncly.domain.file.dto;

import com.project.syncly.domain.s3.enums.FileMimeType;
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

    @Schema(description = "파일 업로드 Presigned URL 요청 DTO")
    public record UploadPresignedUrl(
            @NotNull(message = "폴더 ID는 필수입니다.")
            @Schema(description = "업로드할 폴더 ID")
            Long folderId,

            @NotNull(message = "파일 이름은 필수입니다.")
            @Pattern(
                    regexp = "^[a-zA-Z0-9가-힣_. -]{1,200}$",
                    message = "파일 이름은 1~200자 사이의 한글, 영문, 숫자, 공백, '-', '_', '.'만 사용할 수 있습니다."
            )
            @Schema(description = "파일 이름")
            String fileName,

            @NotNull(message = "파일 MIME 타입은 필수입니다.")
            @Schema(description = "파일 MIME 타입")
            FileMimeType mimeType,

            @Schema(description = "파일 크기(바이트)")
            Long fileSize
    ) {}

    @Schema(description = "파일 업로드 완료 확인 요청 DTO")
    public record ConfirmUpload(
            @NotNull(message = "파일 이름은 필수입니다.")
            @Schema(description = "파일 이름")
            String fileName,

            @NotNull(message = "오브젝트 키는 필수입니다.")
            @Schema(description = "S3 오브젝트 키")
            String objectKey
    ) {}
}