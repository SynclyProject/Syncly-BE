package com.project.syncly.domain.folder.service;

import com.project.syncly.domain.folder.converter.FolderConverter;
import com.project.syncly.domain.folder.dto.FolderRequestDto;
import com.project.syncly.domain.folder.dto.FolderResponseDto;
import com.project.syncly.domain.folder.entity.Folder;
import com.project.syncly.domain.folder.exception.FolderErrorCode;
import com.project.syncly.domain.folder.exception.FolderException;
import com.project.syncly.domain.folder.repository.FolderRepository;
import com.project.syncly.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class FolderCommandServiceImpl implements FolderCommandService{

    private final FolderRepository folderRepository;
    private final FolderClosureCommandService folderClosureCommandService;
    private final WorkspaceRepository workspaceRepository;

    @Override
    public FolderResponseDto.Create create(FolderRequestDto.Create requestDto) {

        // [1] 이름 검증
        String name = requestDto.name();
        if (name == null || name.trim().isEmpty()) {
            throw new FolderException(FolderErrorCode.EMPTY_NAME);
        }
        String nameRegex = "^[a-zA-Z0-9가-힣_-]{1,50}$";
        if (!Pattern.matches(nameRegex, name)) {
            throw new FolderException(FolderErrorCode.INVALID_NAME);
        }

        // [2] 워크스페이스 유효성 검증
        if (!workspaceRepository.existsById(requestDto.workspaceId())) {
            throw new FolderException(FolderErrorCode.WORKSPACE_NOT_FOUND);
        }

        // [3] 부모 폴더가 있을 경우 유효성 검증
        if ((requestDto.parentId() != null && folderRepository.findById(requestDto.parentId()).isEmpty())) {
            throw new FolderException(FolderErrorCode.INVALID_PARENT_FOLDER);
        }

        // [4] 같은 부모 안에 동일한 이름의 폴더 존재하는지 확인
        if (folderRepository.existsByWorkspaceIdAndParentIdAndName(requestDto.workspaceId(), requestDto.parentId(), requestDto.name())) {
            throw new FolderException(FolderErrorCode.DUPLICATE_FOLDER_NAME);
        }

        Folder folder = folderRepository.save(FolderConverter.toFolder(requestDto));
        folderClosureCommandService.updateOnCreate(requestDto.parentId(), folder.getId());
        return FolderConverter.toFolderResponse(folder);
    }
}
