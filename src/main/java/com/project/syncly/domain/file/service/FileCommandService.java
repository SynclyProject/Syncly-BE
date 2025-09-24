package com.project.syncly.domain.file.service;

import com.project.syncly.domain.file.converter.FileConverter;
import com.project.syncly.domain.file.dto.FileRequestDto;
import com.project.syncly.domain.file.dto.FileResponseDto;
import com.project.syncly.domain.file.entity.File;
import com.project.syncly.domain.file.enums.FileType;
import com.project.syncly.domain.file.exception.FileErrorCode;
import com.project.syncly.domain.file.exception.FileException;
import com.project.syncly.domain.file.repository.FileRepository;
import com.project.syncly.domain.folder.entity.Folder;
import com.project.syncly.domain.folder.repository.FolderRepository;
import com.project.syncly.domain.s3.service.S3Service;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import com.project.syncly.domain.workspace.exception.WorkspaceErrorCode;
import com.project.syncly.domain.workspace.exception.WorkspaceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FileCommandService {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final S3Service s3Service;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp",
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt"
    );

    // 파일 업로드
    public FileResponseDto.Upload uploadFile(Long workspaceId, Long folderId, Long workspaceMemberId, MultipartFile multipartFile) {
        validateWorkspaceMembership(workspaceId, workspaceMemberId);
        validateFolder(folderId, workspaceId);
        validateFile(multipartFile);

        String originalFileName = multipartFile.getOriginalFilename();
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new FileException(FileErrorCode.EMPTY_FILE_NAME);
        }

        String uniqueFileName = generateUniqueFileName(folderId, originalFileName);
        FileType fileType = FileType.fromExtension(originalFileName);

        // TODO: S3에 파일 업로드 로직 구현
        String fileUrl = "https://s3.amazonaws.com/syncly-bucket/" + uniqueFileName;

        File file = FileConverter.toFileEntity(folderId, workspaceMemberId, uniqueFileName, fileType, fileUrl, multipartFile);
        File savedFile = fileRepository.save(file);
        return FileConverter.toUploadResponse(savedFile);
    }

    // 파일명 변경
    public FileResponseDto.Update updateFileName(Long workspaceId, Long fileId, Long workspaceMemberId, FileRequestDto.Update requestDto) {
        validateWorkspaceMembership(workspaceId, workspaceMemberId);

        File file = fileRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new FileException(FileErrorCode.FILE_NOT_FOUND));

        validateFolder(file.getFolderId(), workspaceId);

        String newName = requestDto.name();
        if (newName == null || newName.trim().isEmpty()) {
            throw new FileException(FileErrorCode.EMPTY_FILE_NAME);
        }

        if (fileRepository.existsByFolderIdAndNameAndDeletedAtIsNull(file.getFolderId(), newName)
            && !file.getName().equals(newName)) {
            throw new FileException(FileErrorCode.DUPLICATE_FILE_NAME);
        }

        File updatedFile = FileConverter.toUpdatedFileEntity(file, newName);
        File savedFile = fileRepository.save(updatedFile);
        return FileConverter.toUpdateResponse(savedFile);
    }

    // 파일을 휴지통으로 이동 (소프트 삭제)
    public FileResponseDto.Message deleteFile(Long workspaceId, Long fileId, Long workspaceMemberId) {
        validateWorkspaceMembership(workspaceId, workspaceMemberId);

        File file = fileRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new FileException(FileErrorCode.FILE_NOT_FOUND));

        validateFolder(file.getFolderId(), workspaceId);

        File deletedFile = FileConverter.toDeletedFileEntity(file);
        fileRepository.save(deletedFile);
        return FileConverter.toMessageResponse("파일이 휴지통으로 이동되었습니다.");
    }

    // 휴지통에서 파일 복원
    public FileResponseDto.Message restoreFile(Long workspaceId, Long fileId, Long workspaceMemberId) {
        validateWorkspaceMembership(workspaceId, workspaceMemberId);

        File file = fileRepository.findByIdAndDeletedAtIsNotNull(fileId)
                .orElseThrow(() -> new FileException(FileErrorCode.FILE_NOT_FOUND));

        validateFolder(file.getFolderId(), workspaceId);

        // 복원 시 파일명 중복 확인 및 고유한 이름 생성
        String uniqueName = generateUniqueFileName(file.getFolderId(), file.getName());

        File restoredFile = FileConverter.toRestoredFileEntity(file, uniqueName);
        fileRepository.save(restoredFile);

        String message = uniqueName.equals(file.getName()) ?
                "파일이 복원되었습니다." :
                "파일이 복원되었습니다. 중복으로 인해 파일명이 '" + uniqueName + "'으로 변경되었습니다.";

        return FileConverter.toMessageResponse(message);
    }

    // 워크스페이스 멤버십 검증
    private void validateWorkspaceMembership(Long workspaceId, Long workspaceMemberId) {
        // TODO: workspaceMemberId로 직접 확인하는 방법으로 변경 필요
        // 현재는 임시로 memberId로 확인
        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, workspaceMemberId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        }
    }

    // 폴더 존재 여부 및 워크스페이스 소속 검증
    // 요청한 워크스페이스 ID와 폴더가 실제로 속한 워크스페이스 ID가 일치하는지 확인
    private void validateFolder(Long folderId, Long workspaceId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new FileException(FileErrorCode.FORBIDDEN_ACCESS));

        if (!folder.getWorkspaceId().equals(workspaceId)) {
            throw new FileException(FileErrorCode.FORBIDDEN_ACCESS);
        }
    }


    // 업로드 파일 유효성 검증 (크기, 확장자 등)
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileException(FileErrorCode.EMPTY_FILE_NAME);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileException(FileErrorCode.FILE_SIZE_EXCEEDED);
        }

        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.contains(".")) {
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new FileException(FileErrorCode.INVALID_FILE_FORMAT);
            }
        }
    }

    // 폴더 내에서 중복되지 않는 고유한 파일명 생성 (1), (2) .. 순차 증가
    private String generateUniqueFileName(Long folderId, String originalFileName) {
        String baseName = originalFileName;
        String extension = "";

        if (originalFileName.contains(".")) {
            int lastDotIndex = originalFileName.lastIndexOf('.');
            baseName = originalFileName.substring(0, lastDotIndex);
            extension = originalFileName.substring(lastDotIndex);
        }

        String uniqueName = originalFileName;
        int counter = 1;

        while (fileRepository.existsByFolderIdAndNameAndDeletedAtIsNull(folderId, uniqueName)) {
            uniqueName = baseName + "(" + counter + ")" + extension;
            counter++;
        }

        return uniqueName;
    }
}