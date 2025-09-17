package com.project.syncly.domain.file.dto;

import com.project.syncly.domain.folder.dto.PermissionDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class FileResponseDto {

    @Schema(description = "파일 업로드 응답 DTO")
    public record Upload(
            Long id,
            Long folderId,
            String name,
            String type,
            String fileUrl,
            LocalDateTime createdAt
    ){}

    @Schema(description = "파일 이름 변경 응답 DTO")
    public record Update(
            Long id,
            String name,
            LocalDateTime updatedAt
    ){}

    @Schema(description = "파일 삭제/복원 응답 DTO")
    public record Message(
            String message
    ){}

    @Schema(description = "파일 다운로드 응답 DTO")
    public record Download(
            Long id,
            String name,
            String downloadUrl
    ){}

    @Schema(description = "파일 상세 정보 응답 DTO")
    public record Detail(
            Long id,
            String name,
            Long folderId,
            Long workspaceId,
            String type,
            String fileUrl,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            PermissionDto permissions
    ){}
}