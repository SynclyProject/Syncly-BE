package com.project.syncly.domain.folder.service;

import com.project.syncly.domain.folder.dto.FolderResponseDto;
import com.project.syncly.domain.folder.entity.Folder;
import com.project.syncly.domain.folder.exception.FolderErrorCode;
import com.project.syncly.domain.folder.exception.FolderException;
import com.project.syncly.domain.folder.repository.FolderRepository;
import com.project.syncly.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FolderQueryServiceImpl implements FolderQueryService {

    private final FolderRepository folderRepository;
    private final WorkspaceRepository workspaceRepository;

    @Override
    public FolderResponseDto.Root getRootFolder(Long workspaceId) {
        // 워크스페이스 유효성 검증
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new FolderException(FolderErrorCode.WORKSPACE_NOT_FOUND);
        }

        // 루트 폴더 조회
        Folder rootFolder = folderRepository.findByWorkspaceIdAndParentIdIsNull(workspaceId)
                .orElseThrow(() -> new FolderException(FolderErrorCode.FOLDER_NOT_FOUND));

        return new FolderResponseDto.Root(
                rootFolder.getId(),
                rootFolder.getWorkspaceId(),
                rootFolder.getName(),
                rootFolder.getCreatedAt()
        );
    }
}