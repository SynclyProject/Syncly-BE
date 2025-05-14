package com.project.syncly.domain.folder.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class FolderResponseDto {

    @Schema(description = "폴더 생성 응답 DTO")
    public record Create(
            Long id,
            String name,
            Long workspaceId,
            Long parentId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ){}
}
