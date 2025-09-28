package com.project.syncly.domain.file.service;

import com.project.syncly.domain.file.entity.File;
import com.project.syncly.domain.file.exception.FileErrorCode;
import com.project.syncly.domain.file.exception.FileException;
import com.project.syncly.domain.file.repository.FileRepository;
import com.project.syncly.domain.folder.entity.Folder;
import com.project.syncly.domain.folder.repository.FolderRepository;
import com.project.syncly.domain.s3.util.S3Util;
import com.project.syncly.domain.file.dto.FileResponseDto;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import com.project.syncly.domain.workspace.exception.WorkspaceErrorCode;
import com.project.syncly.domain.workspace.exception.WorkspaceException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileQueryService {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final S3Util s3Util;

    // ID로 파일 조회
    public File getFileById(Long workspaceId, Long fileId, Long workspaceMemberId) {
        validateWorkspaceMembership(workspaceId, workspaceMemberId);

        File file = fileRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new FileException(FileErrorCode.FILE_NOT_FOUND));

        validateFolderAccess(file.getFolderId(), workspaceId);

        return file;
    }

    // 폴더별 파일 목록 조회
    public List<File> getFilesByFolder(Long workspaceId, Long folderId, Long workspaceMemberId) {
        validateWorkspaceMembership(workspaceId, workspaceMemberId);
        validateFolderAccess(folderId, workspaceId);

        return fileRepository.findByFolderIdAndDeletedAtIsNull(folderId);
    }

    // 워크스페이스 멤버가 업로드한 파일 목록 조회
    public List<File> getFilesByWorkspaceMember(Long workspaceId, Long workspaceMemberId) {
        validateWorkspaceMembership(workspaceId, workspaceMemberId);

        return fileRepository.findByWorkspaceMemberIdAndDeletedAtIsNull(workspaceMemberId);
    }

    // 워크스페이스 내 모든 파일 조회
    public List<File> getAllFilesByWorkspace(Long workspaceId, Long workspaceMemberId) {
        validateWorkspaceMembership(workspaceId, workspaceMemberId);

        return fileRepository.findAllByWorkspaceIdThroughFolder(workspaceId);
    }

    // 파일 다운로드 URL 생성
    public FileResponseDto.DownloadUrl getFileDownloadUrl(Long workspaceId, Long fileId, Long workspaceMemberId) {
        File file = getFileById(workspaceId, fileId, workspaceMemberId);

        try {
            String downloadUrl = s3Util.createPresignedGetUrlForDownload(file.getObjectKey(), file.getName());

            // 파일 상세 정보 생성 (보안을 고려하여 필요한 정보만)
            FileResponseDto.FileInfo fileInfo = new FileResponseDto.FileInfo(
                    file.getId(),
                    file.getName(),
                    file.getType().getKey(),
                    file.getSize(),
                    file.getCreatedAt()
            );

            return new FileResponseDto.DownloadUrl(downloadUrl, file.getName(), fileInfo);
        } catch (Exception e) {
            throw new FileException(FileErrorCode.FILE_DOWNLOAD_FAILED);
        }
    }

    // 폴더 내 파일명 중복 여부 확인
    public boolean existsByFolderAndName(Long folderId, String name) {
        return fileRepository.existsByFolderIdAndNameAndDeletedAtIsNull(folderId, name);
    }

    // 워크스페이스 휴지통 파일 조회 (내부 사용용)
    public java.util.List<File> getTrashFileEntities(Long workspaceId, Long workspaceMemberId) {
        validateWorkspaceMembership(workspaceId, workspaceMemberId);
        return fileRepository.findTrashFilesByWorkspaceId(workspaceId);
    }

    // 워크스페이스 멤버십 검증
    private void validateWorkspaceMembership(Long workspaceId, Long workspaceMemberId) {
        // TODO: workspaceMemberId로 직접 확인하는 방법으로 변경 필요
        // 현재는 임시로 memberId로 확인
        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, workspaceMemberId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        }
    }

    // 폴더 접근 권한 검증
    private void validateFolderAccess(Long folderId, Long workspaceId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new FileException(FileErrorCode.FORBIDDEN_ACCESS));

        if (!folder.getWorkspaceId().equals(workspaceId)) {
            throw new FileException(FileErrorCode.FORBIDDEN_ACCESS);
        }
    }
}