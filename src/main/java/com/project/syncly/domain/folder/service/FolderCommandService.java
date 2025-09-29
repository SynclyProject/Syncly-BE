package com.project.syncly.domain.folder.service;

import com.project.syncly.domain.folder.dto.FolderRequestDto;
import com.project.syncly.domain.folder.dto.FolderResponseDto;

public interface FolderCommandService {
    FolderResponseDto.Create create(Long workspaceID, FolderRequestDto.Create requestDto, Long memberId);

    FolderResponseDto.Create createRootFolder(Long workspaceId);

    FolderResponseDto.Update updateFolderName(Long workspaceId, Long folderId, FolderRequestDto.Update requestDto, Long memberId);

    FolderResponseDto.Message deleteFolder(Long workspaceId, Long folderId, Long memberId);

    FolderResponseDto.Message restoreFolder(Long workspaceId, Long folderId, Long memberId);

    FolderResponseDto.Message hardDeleteFolder(Long workspaceId, Long folderId, Long memberId);
}
