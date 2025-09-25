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
                    regexp = "^[a-zA-Z0-9가-힣_. -]{1,100}$",
                    message = "파일 이름은 1~100자 사이의 한글, 영문, 숫자, 공백, '-', '_', '.'만 사용할 수 있습니다."
            )
            @Schema(
                    description = "새로운 파일명 (확장자 포함 필수)",
                    example = "수정된파일명.pdf",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String name
    ) {}

    @Schema(description = "파일 업로드 Presigned URL 요청 DTO - S3 업로드를 위한 임시 URL 발급 요청")
    public record UploadPresignedUrl(
            @NotNull(message = "폴더 ID는 필수입니다.")
            @Schema(
                    description = "업로드할 폴더 ID",
                    example = "1"
            )
            Long folderId,

            @NotNull(message = "파일 이름은 필수입니다.")
            @Pattern(
                    regexp = "^[a-zA-Z0-9가-힣_. -]{1,200}$",
                    message = "파일 이름은 1~200자 사이의 한글, 영문, 숫자, 공백, '-', '_', '.'만 사용할 수 있습니다."
            )
            @Schema(
                    description = "파일 이름 (확장자 포함 필수, MIME 타입 자동 추출)",
                    example = "보고서.pdf",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String fileName,

            @Schema(
                    description = "파일 크기 (바이트 단위, 선택사항)",
                    example = "10485760",
                    minimum = "1",
                    maximum = "209715200"
            )
            Long fileSize
    ) {}

    @Schema(description = "파일 업로드 완료 확인 요청 DTO")
    public record ConfirmUpload(
            @NotNull(message = "파일 이름은 필수입니다.")
            @Schema(
                    description = "파일 이름 (확장자 포함 필수)",
                    example = "보고서.pdf",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String fileName,

            @NotNull(message = "오브젝트 키는 필수입니다.")
            @Schema(description = "S3 오브젝트 키")
            String objectKey
    ) {}
}