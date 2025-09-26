package com.project.syncly.domain.folder.service;

import com.project.syncly.domain.folder.dto.FolderResponseDto;
import com.project.syncly.domain.folder.entity.Folder;
import com.project.syncly.domain.folder.exception.FolderErrorCode;
import com.project.syncly.domain.folder.exception.FolderException;
import com.project.syncly.domain.folder.repository.FolderRepository;
import com.project.syncly.domain.folder.repository.FolderClosureRepository;
import com.project.syncly.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FolderQueryServiceImpl implements FolderQueryService {

    private final FolderRepository folderRepository;
    private final FolderClosureRepository folderClosureRepository;
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

    @Override
    public FolderResponseDto.Path getFolderPath(Long workspaceId, Long folderId) {
        // 폴더가 해당 워크스페이스에 속하는지 확인
        if (!folderRepository.existsByWorkspaceIdAndId(workspaceId, folderId)) {
            throw new FolderException(FolderErrorCode.FOLDER_NOT_FOUND);
        }

        // FolderClosure 테이블을 활용해 루트부터 현재 폴더까지의 전체 경로 조회
        List<Object[]> pathData = folderClosureRepository.findPathFromRoot(folderId);

        // Object[] 배열을 FolderResponseDto.PathItem으로 변환
        List<FolderResponseDto.PathItem> pathItems = pathData.stream()
                .map(row -> new FolderResponseDto.PathItem(
                        (Long) row[0],   // id
                        (String) row[1]  // name
                ))
                .collect(Collectors.toList());

        return new FolderResponseDto.Path(pathItems);
    }
}