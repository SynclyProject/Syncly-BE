package com.project.syncly.domain.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 노트 이미지 업로드 관련 DTO 모음
 */
public class NoteImageDto {

    /**
     * Presigned URL 발급 요청 DTO
     */
    @Schema(description = "Presigned URL 발급 요청 DTO")
    public record PresignedUrlRequest(
            @Schema(description = "원본 파일명", example = "screenshot.png")
            @NotBlank(message = "파일명은 필수입니다")
            @Size(max = 255, message = "파일명은 255자를 초과할 수 없습니다")
            String filename,

            @Schema(description = "파일 Content Type (image/* 형식)", example = "image/png")
            @NotBlank(message = "Content Type은 필수입니다")
            @Pattern(regexp = "^image/.*", message = "이미지 파일만 업로드 가능합니다")
            String contentType,

            @Schema(description = "파일 크기 (바이트)", example = "1048576")
            @NotNull(message = "파일 크기는 필수입니다")
            Long fileSize
    ) {
        /**
         * 파일 크기 검증 (최대 10MB)
         */
        public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

        /**
         * 파일명 정제 (특수문자 제거)
         */
        public String sanitizeFilename() {
            if (filename == null) {
                return "unknown";
            }
            // 위험한 문자 제거: 경로 구분자, null byte, 제어 문자 등
            String sanitized = filename.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F]", "_");
            // 연속된 점 제거 (경로 탐색 방지)
            sanitized = sanitized.replaceAll("\\.{2,}", ".");
            // 앞뒤 공백 및 점 제거
            sanitized = sanitized.trim().replaceAll("^\\.+|\\.+$", "");

            if (sanitized.isEmpty()) {
                return "unknown";
            }

            return sanitized;
        }

        /**
         * 파일 크기 검증
         */
        public void validateFileSize() {
            if (fileSize > MAX_FILE_SIZE) {
                throw new IllegalArgumentException(
                        String.format("파일 크기는 %dMB를 초과할 수 없습니다", MAX_FILE_SIZE / 1024 / 1024)
                );
            }
        }
    }

    /**
     * Presigned URL 발급 응답 DTO
     */
    @Schema(description = "Presigned URL 발급 응답 DTO")
    public record PresignedUrlResponse(
            @Schema(description = "업로드용 Presigned URL")
            String uploadUrl,

            @Schema(description = "이미지 ID (업로드 확인 시 사용)")
            Long imageId,

            @Schema(description = "S3 Object Key")
            String objectKey,

            @Schema(description = "Presigned URL 만료 시간")
            LocalDateTime expiresAt
    ) {}

    /**
     * 이미지 업로드 확인 요청 DTO
     */
    @Schema(description = "이미지 업로드 확인 요청 DTO")
    public record ImageConfirmRequest(
            @Schema(description = "이미지 ID", example = "123")
            @NotNull(message = "이미지 ID는 필수입니다")
            Long imageId
    ) {}

    /**
     * 이미지 URL 응답 DTO
     */
    @Schema(description = "이미지 URL 응답 DTO")
    public record ImageUrlResponse(
            @Schema(description = "이미지 공개 URL (CloudFront 또는 S3 URL)")
            String imageUrl,

            @Schema(description = "마크다운 문법", example = "![image](https://...)")
            String markdownSyntax
    ) {
        /**
         * 이미지 URL로부터 응답 생성
         */
        public static ImageUrlResponse from(String imageUrl, String originalFilename) {
            String markdown = String.format("![%s](%s)", originalFilename, imageUrl);
            return new ImageUrlResponse(imageUrl, markdown);
        }
    }

    /**
     * 이미지 삭제 응답 DTO
     */
    @Schema(description = "이미지 삭제 응답 DTO")
    public record ImageDeleteResponse(
            @Schema(description = "삭제된 이미지 ID")
            Long imageId,

            @Schema(description = "성공 메시지")
            String message
    ) {
        public static ImageDeleteResponse of(Long imageId) {
            return new ImageDeleteResponse(imageId, "이미지가 성공적으로 삭제되었습니다");
        }
    }
}
