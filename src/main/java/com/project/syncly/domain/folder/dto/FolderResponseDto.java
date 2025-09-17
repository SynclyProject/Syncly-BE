package com.project.syncly.domain.folder.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

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

    @Schema(description = "폴더 이름 변경 응답 DTO")
    public record Update(
            Long id,
            String name,
            LocalDateTime updatedAt
    ){}

    @Schema(description = "폴더 삭제/복원 응답 DTO")
    public record Message(
            String message
    ){}

    @Schema(description = "폴더 경로 조회 응답 DTO")
    public record Path(
            List<PathItem> path
    ){}

    @Schema(description = "폴더 경로 아이템 DTO")
    public record PathItem(
            Long id,
            String name
    ){}

    @Schema(description = "폴더/파일 목록 조회 응답 DTO")
    public record ItemList(
            List<ListingDto.Item> items,
            String nextCursor,
            PermissionDto permissions
    ){}

    @Schema(description = "폴더 상세 정보 응답 DTO")
    public record Detail(
            Long id,
            String name,
            Long workspaceId,
            Long parentId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            PermissionDto permissions
    ){}

    @Schema(description = "워크스페이스 루트 폴더 정보 응답 DTO")
    public record Root(
            Long rootFolderId,
            Long workspaceId,
            String name,
            LocalDateTime createdAt
    ){}
}
