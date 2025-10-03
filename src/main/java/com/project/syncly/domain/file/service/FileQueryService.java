package com.project.syncly.domain.file.service;

import com.project.syncly.domain.file.entity.File;
import com.project.syncly.domain.file.dto.FileResponseDto;

import java.util.List;

public interface FileQueryService {
    File getFileById(Long workspaceId, Long fileId, Long workspaceMemberId);

    List<File> getFilesByFolder(Long workspaceId, Long folderId, Long workspaceMemberId);

    List<File> getFilesByWorkspaceMember(Long workspaceId, Long workspaceMemberId);

    List<File> getAllFilesByWorkspace(Long workspaceId, Long workspaceMemberId);

    FileResponseDto.DownloadUrl getFileDownloadUrl(Long workspaceId, Long fileId, Long workspaceMemberId);

    boolean existsByFolderAndName(Long folderId, String name);

    List<File> getTrashFileEntities(Long workspaceId, Long workspaceMemberId);
}
