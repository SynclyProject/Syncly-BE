package com.project.syncly.domain.folder.service;

import com.project.syncly.domain.folder.dto.FolderResponseDto;

public interface FolderQueryService {
    FolderResponseDto.Root getRootFolder(Long workspaceId);
    FolderResponseDto.Path getFolderPath(Long workspaceId, Long folderId);
}