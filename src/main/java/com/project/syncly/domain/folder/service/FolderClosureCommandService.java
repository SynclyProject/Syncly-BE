package com.project.syncly.domain.folder.service;

public interface FolderClosureCommandService {
    void updateOnCreate(Long parentId, Long newFolderId);
}
