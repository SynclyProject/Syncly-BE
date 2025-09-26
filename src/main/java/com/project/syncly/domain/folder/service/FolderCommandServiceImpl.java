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

        // [3] 부모 폴더 처리 및 유효성 검사
        if (parentId == null) {
            // parentId가 null이면 워크스페이스 루트 폴더를 부모로 설정
            Folder rootFolder = folderRepository.findByWorkspaceIdAndParentIdIsNull(workspaceId)
                    .orElseThrow(() -> new FolderException(FolderErrorCode.FOLDER_NOT_FOUND));
            parentId = rootFolder.getId();
        } else {
            // 부모 폴더 존재 여부 확인
            if (!folderRepository.existsById(parentId)) {
                throw new FolderException(FolderErrorCode.FOLDER_NOT_FOUND);
            }

            // 부모 폴더가 같은 워크스페이스에 속하는지 확인
            if (!folderRepository.existsByWorkspaceIdAndId(workspaceId, parentId)) {
                throw new FolderException(FolderErrorCode.INVALID_PARENT_FOLDER);
            }

            // 깊이 제한 검사
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
        if (folderRepository.existsByWorkspaceIdAndParentIdAndName(workspaceId, parentId, requestDto.name())) {
            throw new FolderException(FolderErrorCode.DUPLICATE_FOLDER_NAME);
        }

        // [5] 폴더 생성 - parentId를 업데이트된 값으로 사용
        FolderRequestDto.Create updatedRequestDto = new FolderRequestDto.Create(parentId, requestDto.name());
        Folder folder = folderRepository.save(FolderConverter.toFolder(workspaceId, updatedRequestDto, memberId));
        folderClosureCommandService.updateOnCreate(parentId, folder.getId());
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

    @Override
    public FolderResponseDto.Update updateFolderName(Long workspaceId, Long folderId, FolderRequestDto.Update requestDto, Long memberId) {

        // [1] 워크스페이스 회원 여부 검증
        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, memberId)) {
            throw new FolderException(FolderErrorCode.FORBIDDEN_ACCESS);
        }

        // [2] 이름 검증
        String newName = requestDto.name();
        if (newName == null || newName.trim().isEmpty()) {
            throw new FolderException(FolderErrorCode.EMPTY_NAME);
        }
        String nameRegex = "^[a-zA-Z0-9가-힣_-]{1,50}$";
        if (!Pattern.matches(nameRegex, newName)) {
            throw new FolderException(FolderErrorCode.INVALID_NAME);
        }

        // [3] 폴더 존재 여부 및 워크스페이스 소속 확인
        Folder folder = folderRepository.findByIdAndWorkspaceId(folderId, workspaceId)
                .orElseThrow(() -> new FolderException(FolderErrorCode.FOLDER_NOT_FOUND));

        // [4] 같은 부모 폴더 하위에서 새 이름 중복 체크 (자기 자신 제외)
        if (folderRepository.existsByWorkspaceIdAndParentIdAndName(workspaceId, folder.getParentId(), newName)) {
            // 현재 폴더 이름과 같다면 중복이 아님
            if (!folder.getName().equals(newName)) {
                throw new FolderException(FolderErrorCode.DUPLICATE_FOLDER_NAME);
            }
        }

        // [5] 폴더 이름 업데이트
        folder.updateName(newName);
        Folder updatedFolder = folderRepository.save(folder);

        return new FolderResponseDto.Update(
                updatedFolder.getId(),
                updatedFolder.getName(),
                updatedFolder.getUpdatedAt()
        );
    }
}
