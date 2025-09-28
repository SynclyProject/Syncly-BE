package com.project.syncly.domain.folder.service;

import com.project.syncly.domain.folder.dto.FolderResponseDto;

public interface FolderQueryService {
    FolderResponseDto.Root getRootFolder(Long workspaceId);
    FolderResponseDto.Path getFolderPath(Long workspaceId, Long folderId);
    FolderResponseDto.ItemList getFolderItems(Long workspaceId, Long folderId, String sort, String cursor, Integer limit, String search, Long uploaderId);
    FolderResponseDto.ItemList getTrashItems(Long workspaceId, String sort, String cursor, Integer limit, String search, Long uploaderId);
}