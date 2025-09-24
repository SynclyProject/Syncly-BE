package com.project.syncly.domain.folder.service;

import com.project.syncly.domain.folder.converter.FolderConverter;
import com.project.syncly.domain.folder.dto.FolderRequestDto;
import com.project.syncly.domain.folder.dto.FolderResponseDto;
import com.project.syncly.domain.folder.entity.Folder;
import com.project.syncly.domain.folder.entity.FolderClosure;
import com.project.syncly.domain.folder.entity.FolderDepth;
import com.project.syncly.domain.folder.exception.FolderErrorCode;
import com.project.syncly.domain.folder.exception.FolderException;
import com.project.syncly.domain.folder.repository.FolderClosureRepository;
import com.project.syncly.domain.folder.repository.FolderRepository;
import com.project.syncly.domain.workspace.repository.WorkspaceRepository;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FolderCommandServiceImpl implements FolderCommandService{

    private final FolderRepository folderRepository;
    private final FolderClosureRepository folderClosureRepository;
    private final FolderClosureCommandService folderClosureCommandService;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    public FolderResponseDto.Create create(Long workspaceId, FolderRequestDto.Create requestDto, Long memberId) {

        Long parentId = requestDto.parentId();
        log.info("memberId: {}", memberId);

        // [0] 워크스페이스 회원 여부 검증
        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, memberId)) {
            throw new FolderException(FolderErrorCode.FORBIDDEN_ACCESS);
        }

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
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new FolderException(FolderErrorCode.WORKSPACE_NOT_FOUND);
        }

        // [3] 부모 폴더 유효성 검사 & 깊이 제한
        if (parentId != null) {
            if (folderRepository.findById(parentId).isEmpty()) {
                throw new FolderException(FolderErrorCode.INVALID_PARENT_FOLDER);
            }

            int parentDepth = folderClosureRepository
                    .findByDescendantId(parentId).stream()
                    .mapToInt(FolderClosure::getDepth)
                    .max()
                    .orElse(0);

            if (parentDepth + 1 > FolderDepth.MAX.getValue()) {
                throw new FolderException(FolderErrorCode.FOLDER_DEPTH_EXCEEDED);
            }
        }

        // [4] 같은 부모 안에 동일한 이름의 폴더 존재하는지 확인
        if (folderRepository.existsByWorkspaceIdAndParentIdAndName(workspaceId, requestDto.parentId(), requestDto.name())) {
            throw new FolderException(FolderErrorCode.DUPLICATE_FOLDER_NAME);
        }

        Folder folder = folderRepository.save(FolderConverter.toFolder(workspaceId, requestDto));
        folderClosureCommandService.updateOnCreate(requestDto.parentId(), folder.getId());
        return FolderConverter.toFolderResponse(folder);
    }

    @Override
    public FolderResponseDto.Create createRootFolder(Long workspaceId) {
        // 이미 루트 폴더가 있으면 예외 처리
        if (folderRepository.existsByWorkspaceIdAndParentIdIsNull(workspaceId)) {
            throw new FolderException(FolderErrorCode.ROOT_FOLDER_ALREADY_EXISTS);
        }

        // 워크스페이스 유효성 검증
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new FolderException(FolderErrorCode.WORKSPACE_NOT_FOUND);
        }

        // 루트 폴더 생성 (parentId는 null, name은 "root")
        Folder rootFolder = Folder.builder()
                .workspaceId(workspaceId)
                .parentId(null)
                .name("root")
                .build();

        Folder savedFolder = folderRepository.save(rootFolder);

        // FolderClosure 테이블에 자기 자신에 대한 경로(depth=0) 추가
        folderClosureCommandService.updateOnCreate(null, savedFolder.getId());

        return FolderConverter.toFolderResponse(savedFolder);
    }
}
