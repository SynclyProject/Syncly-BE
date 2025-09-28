package com.project.syncly.domain.file.dto;

import com.project.syncly.domain.folder.dto.PermissionDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class FileResponseDto {

    @Schema(description = "파일 업로드 응답 DTO")
    public record Upload(
            @Schema(description = "파일 ID")
            Long id,
            @Schema(description = "파일명")
            String name,
            @Schema(description = "파일 타입")
            String type,
            @Schema(description = "파일 크기 (바이트)")
            Long size,
            @Schema(description = "생성 일시")
            LocalDateTime createdAt
    ){}

    @Schema(description = "파일 이름 변경 응답 DTO")
    public record Update(
            @Schema(description = "파일 ID")
            Long id,
            @Schema(description = "수정된 파일명")
            String name,
            @Schema(description = "수정 일시")
            LocalDateTime updatedAt
    ){}

    @Schema(description = "파일 삭제/복원 응답 DTO")
    public record Message(
            String message
    ){}

    @Schema(description = "파일 다운로드 URL 응답 DTO")
    public record DownloadUrl(
            @Schema(description = "임시 다운로드 URL (5분 유효)")
            String downloadUrl,
            @Schema(description = "파일 이름")
            String fileName,
            @Schema(description = "파일 상세 정보")
            FileInfo fileInfo
    ){}

    @Schema(description = "파일 정보 DTO")
    public record FileInfo(
            @Schema(description = "파일 ID")
            Long id,
            @Schema(description = "파일명")
            String name,
            @Schema(description = "파일 타입")
            String type,
            @Schema(description = "파일 크기 (바이트)")
            Long size,
            @Schema(description = "생성 일시")
            LocalDateTime createdAt
    ){}

    @Schema(description = "파일 상세 정보 응답 DTO")
    public record Detail(
            Long id,
            String name,
            Long folderId,
            Long workspaceId,
            String type,
            String objectKey,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            PermissionDto permissions
    ){}

    @Schema(description = "파일 업로드 Presigned URL 응답 DTO")
    public record PresignedUrl(
            @Schema(description = "파일 이름")
            String fileName,

            @Schema(description = "S3 업로드용 Presigned URL")
            String presignedUrl,

            @Schema(description = "S3 오브젝트 키")
            String objectKey
    ){}
}