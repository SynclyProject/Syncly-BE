package com.project.syncly.domain.folder.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "권한 정보 DTO")
public record PermissionDto(
        @Schema(description = "읽기 권한 여부")
        boolean canRead,

        @Schema(description = "쓰기 권한 여부")
        boolean canWrite,

        @Schema(description = "삭제 권한 여부")
        boolean canDelete,

        @Schema(description = "관리자 권한 여부")
        boolean canAdmin
) {}