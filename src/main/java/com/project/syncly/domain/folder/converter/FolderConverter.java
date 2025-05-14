package com.project.syncly.domain.folder.converter;

import com.project.syncly.domain.folder.dto.FolderRequestDto;
import com.project.syncly.domain.folder.dto.FolderResponseDto;
import com.project.syncly.domain.folder.entity.Folder;
import com.project.syncly.domain.folder.entity.FolderClosure;

public class FolderConverter {
    public static Folder toFolder(FolderRequestDto.Create dto) {
        return Folder.builder()
                .workspaceId(dto.workspaceId())
                .parentId(dto.parentId())
                .name(dto.name())
                .build();
    }

    public static FolderResponseDto.Create toFolderResponse(Folder folder) {
        return new FolderResponseDto.Create(
                folder.getId(),
                folder.getName(),
                folder.getWorkspaceId(),
                folder.getParentId(),
                folder.getCreatedAt(),
                folder.getUpdatedAt()
        );
    }
    public static FolderClosure toFolderClosure(Long ancestorId, Long descendantId, int depth) {
        return FolderClosure.builder()
                .ancestorId(ancestorId)
                .descendantId(descendantId)
                .depth(depth)
                .build();
    }

}
