package com.project.syncly.domain.file.service;

import com.project.syncly.domain.file.dto.FileRequestDto;
import com.project.syncly.domain.file.dto.FileResponseDto;

public interface FileCommandService {
    FileResponseDto.PresignedUrl generatePresignedUrl(Long workspaceId, Long folderId, Long workspaceMemberId, FileRequestDto.UploadPresignedUrl requestDto);

    FileResponseDto.Upload confirmFileUpload(Long workspaceId, Long workspaceMemberId, FileRequestDto.ConfirmUpload requestDto);

    FileResponseDto.Update updateFileName(Long workspaceId, Long fileId, Long workspaceMemberId, FileRequestDto.Update requestDto);

    FileResponseDto.Message deleteFile(Long workspaceId, Long fileId, Long workspaceMemberId);

    FileResponseDto.Message restoreFile(Long workspaceId, Long fileId, Long workspaceMemberId);

    FileResponseDto.Message hardDeleteFile(Long workspaceId, Long fileId, Long workspaceMemberId);
}
