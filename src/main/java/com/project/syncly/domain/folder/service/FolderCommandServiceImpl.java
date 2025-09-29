package com.project.syncly.domain.folder.service;

import com.project.syncly.domain.file.repository.FileRepository;
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
import com.project.syncly.domain.workspaceMember.entity.WorkspaceMember;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
    private final FileRepository fileRepository;

    @Override
    public FolderResponseDto.Create create(Long workspaceId, FolderRequestDto.Create requestDto, Long memberId) {

        Long parentId = requestDto.parentId();
        log.info("memberId: {}", memberId);

        // [0] 워크스페이스 회원 여부 검증 및 실제 WorkspaceMember ID 조회
        WorkspaceMember workspaceMember = workspaceMemberRepository
            .findByWorkspaceIdAndMemberId(workspaceId, memberId)
            .orElseThrow(() -> new FolderException(FolderErrorCode.FORBIDDEN_ACCESS));

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

        // [5] 폴더 생성 - parentId를 업데이트된 값으로 사용, 올바른 workspaceMemberId 사용
        FolderRequestDto.Create updatedRequestDto = new FolderRequestDto.Create(parentId, requestDto.name());
        Folder folder = folderRepository.save(FolderConverter.toFolder(workspaceId, updatedRequestDto, workspaceMember.getId()));
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

        // [1] 워크스페이스 회원 여부 검증 및 실제 WorkspaceMember ID 조회
        WorkspaceMember workspaceMember = workspaceMemberRepository
            .findByWorkspaceIdAndMemberId(workspaceId, memberId)
            .orElseThrow(() -> new FolderException(FolderErrorCode.FORBIDDEN_ACCESS));

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

    @Override
    public FolderResponseDto.Message deleteFolder(Long workspaceId, Long folderId, Long memberId) {

        // [1] 워크스페이스 회원 여부 검증 및 실제 WorkspaceMember ID 조회
        WorkspaceMember workspaceMember = workspaceMemberRepository
            .findByWorkspaceIdAndMemberId(workspaceId, memberId)
            .orElseThrow(() -> new FolderException(FolderErrorCode.FORBIDDEN_ACCESS));

        // [2] 폴더 존재 여부 및 워크스페이스 소속 확인
        Folder folder = folderRepository.findByIdAndWorkspaceId(folderId, workspaceId)
                .orElseThrow(() -> new FolderException(FolderErrorCode.FOLDER_NOT_FOUND));

        // [3] 루트 폴더 삭제 금지
        if (folder.getParentId() == null) {
            throw new FolderException(FolderErrorCode.ROOT_FOLDER_DELETE_FORBIDDEN);
        }

        // [4] 모든 하위 폴더 ID 조회 (자기 자신 포함)
        List<Long> descendantFolderIds = folderClosureRepository.findAllDescendantIds(folderId);

        // [5] 트랜잭션으로 cascade 삭제 수행
        LocalDateTime deletedAt = LocalDateTime.now();

        // 하위 폴더들의 파일을 모두 soft delete
        if (!descendantFolderIds.isEmpty()) {
            fileRepository.updateDeletedAtByFolderIdIn(descendantFolderIds, deletedAt);
        }

        // 폴더들을 모두 soft delete
        if (!descendantFolderIds.isEmpty()) {
            folderRepository.updateDeletedAtByIdIn(descendantFolderIds, deletedAt);
        }

        // FolderClosure에서 관련 경로들 삭제
        for (Long descendantId : descendantFolderIds) {
            folderClosureRepository.deleteByAncestorIdOrDescendantId(descendantId);
        }

        log.info("Folder {} and {} descendants deleted successfully", folderId, descendantFolderIds.size() - 1);

        return new FolderResponseDto.Message("폴더가 휴지통으로 이동되었습니다.");
    }

    @Override
    public FolderResponseDto.Message restoreFolder(Long workspaceId, Long folderId, Long memberId) {

        // [1] 워크스페이스 회원 여부 검증 및 실제 WorkspaceMember ID 조회
        WorkspaceMember workspaceMember = workspaceMemberRepository
            .findByWorkspaceIdAndMemberId(workspaceId, memberId)
            .orElseThrow(() -> new FolderException(FolderErrorCode.FORBIDDEN_ACCESS));

        // [2] 삭제된 폴더 존재 여부 확인
        Folder folder = folderRepository.findDeletedByIdAndWorkspaceId(folderId, workspaceId)
                .orElseThrow(() -> new FolderException(FolderErrorCode.FOLDER_NOT_FOUND));

        if (folder.getDeletedAt() == null) {
            throw new FolderException(FolderErrorCode.FOLDER_NOT_DELETED);
        }

        // [3] 복원할 부모 폴더 결정
        Long targetParentId = folder.getParentId();

        // 부모 폴더가 삭제되었거나 존재하지 않으면 루트 폴더로 복원
        if (targetParentId != null) {
            Optional<Folder> parentFolder = folderRepository.findByIdAndWorkspaceId(targetParentId, workspaceId);
            if (!parentFolder.isPresent() || parentFolder.get().getDeletedAt() != null) {
                // 루트 폴더를 찾아서 부모로 설정
                Folder rootFolder = folderRepository.findByWorkspaceIdAndParentIdIsNull(workspaceId)
                        .orElseThrow(() -> new FolderException(FolderErrorCode.FOLDER_NOT_FOUND));
                targetParentId = rootFolder.getId();
            }
        }

        // [4] 순환 참조 방지 검사
        if (targetParentId != null && targetParentId.equals(folderId)) {
            throw new FolderException(FolderErrorCode.CIRCULAR_REFERENCE_ERROR);
        }

        // [5] 폴더명 중복 체크 및 필요시 이름 변경
        String originalName = folder.getName();
        String restoreName = originalName;

        if (folderRepository.existsByWorkspaceIdAndParentIdAndNameAndDeletedAtIsNull(workspaceId, targetParentId, originalName)) {
            // 중복되는 경우 타임스탬프를 추가한 이름으로 변경
            long timestamp = System.currentTimeMillis();
            restoreName = originalName + "_복원_" + timestamp;
        }

        // [6] 삭제된 폴더들 직접 조회 (FolderClosure가 삭제되어 있으므로)
        List<Long> descendantFolderIds = findDeletedDescendants(folderId);

        // [7] 트랜잭션으로 복원 수행
        // 폴더들을 복원
        if (!descendantFolderIds.isEmpty()) {
            folderRepository.restoreByIdIn(descendantFolderIds);
        }

        // 하위 폴더들의 파일도 복원
        if (!descendantFolderIds.isEmpty()) {
            fileRepository.restoreByFolderIdIn(descendantFolderIds);
        }

        // 복원된 폴더의 parentId와 name 업데이트 (필요한 경우)
        if (!targetParentId.equals(folder.getParentId()) || !restoreName.equals(originalName)) {
            folderRepository.updateParentIdAndName(folderId, targetParentId, restoreName);
        }

        // [8] FolderClosure 관계 재구성
        // 모든 하위 폴더들에 대해 FolderClosure 관계 재설정
        for (Long descendantId : descendantFolderIds) {
            // 기존 관계가 있다면 삭제하고 다시 생성
            if (descendantId.equals(folderId)) {
                // 복원되는 폴더의 경우 새로운 부모 관계로 재구성
                folderClosureCommandService.updateOnCreate(targetParentId, descendantId);
            } else {
                // 하위 폴더들은 현재 구조 그대로 재구성
                Folder descendantFolder = folderRepository.findById(descendantId)
                        .orElseThrow(() -> new FolderException(FolderErrorCode.FOLDER_NOT_FOUND));
                folderClosureCommandService.updateOnCreate(descendantFolder.getParentId(), descendantId);
            }
        }

        log.info("Folder {} and {} descendants restored successfully", folderId, descendantFolderIds.size() - 1);

        return new FolderResponseDto.Message("폴더가 복원되었습니다.");
    }

    // 삭제된 폴더의 하위 폴더들을 재귀적으로 찾는 헬퍼 메서드
    private List<Long> findDeletedDescendants(Long folderId) {
        List<Long> allIds = new java.util.ArrayList<>();
        allIds.add(folderId); // 자기 자신 포함

        findDeletedDescendantsRecursive(folderId, allIds);

        return allIds;
    }

    private void findDeletedDescendantsRecursive(Long parentId, List<Long> allIds) {
        // deletedAt이 NULL이 아닌 하위 폴더들 찾기
        List<Folder> deletedChildren = folderRepository.findByParentIdAndDeletedAtIsNotNull(parentId);

        for (Folder child : deletedChildren) {
            allIds.add(child.getId());
            // 재귀적으로 하위 폴더들 찾기
            findDeletedDescendantsRecursive(child.getId(), allIds);
        }
    }
}
