package com.project.syncly.domain.folder.service;

import com.project.syncly.domain.folder.dto.FolderRequestDto;
import com.project.syncly.domain.folder.dto.FolderResponseDto;

public interface FolderCommandService {
    FolderResponseDto.Create create(Long workspaceID, FolderRequestDto.Create requestDto, Long memberId);
}
