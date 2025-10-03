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
import com.project.syncly.domain.s3.enums.FileMimeType;
import com.project.syncly.domain.s3.util.S3Util;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import com.project.syncly.domain.workspace.exception.WorkspaceErrorCode;
import com.project.syncly.domain.workspace.exception.WorkspaceException;
import com.project.syncly.global.redis.core.RedisStorage;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional
public class FileCommandServiceImpl implements FileCommandService {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final S3Util s3Util;
    private final RedisStorage redisStorage;

    private static final long MAX_FILE_SIZE = 500 * 1024 * 1024; // 500MB

    @Override
    public FileResponseDto.PresignedUrl generatePresignedUrl(Long workspaceId, Long folderId, Long workspaceMemberId, FileRequestDto.UploadPresignedUrl requestDto) {
        validateWorkspaceMembership(workspaceId, workspaceMemberId);
        validateFolder(folderId, workspaceId);
        validateFileRequest(requestDto);
        validateFileNameHasExtension(requestDto.fileName());

        String uniqueFileName = generateUniqueFileName(folderId, requestDto.fileName());
        String objectKey = "uploads/" + java.util.UUID.randomUUID() + "_" + uniqueFileName;

        // 파일명에서 mimeType 자동 추출
        FileMimeType mimeType = FileMimeType.extractMimeType(uniqueFileName);

        String presignedUrl = s3Util.createPresignedUrl(objectKey, mimeType);

        // Redis에 업로드 권한 정보 저장 (보안 검증용)
        String redisKey = RedisKeyPrefix.S3_AUTH_OBJECT_KEY.get(workspaceMemberId + ":" + uniqueFileName + ":" + objectKey);
        FileRequestDto.UploadPresignedUrl uploadInfo = new FileRequestDto.UploadPresignedUrl(
                folderId, uniqueFileName, requestDto.fileSize()
        );
        redisStorage.set(redisKey, uploadInfo, Duration.ofMinutes(10));

        return new FileResponseDto.PresignedUrl(uniqueFileName, presignedUrl, objectKey);
    }

    @Override
    public FileResponseDto.Upload confirmFileUpload(Long workspaceId, Long workspaceMemberId, FileRequestDto.ConfirmUpload requestDto) {
        validateWorkspaceMembership(workspaceId, workspaceMemberId);
        validateFileNameHasExtension(requestDto.fileName());

        // Redis에서 업로드 권한 검증
        String redisKey = RedisKeyPrefix.S3_AUTH_OBJECT_KEY.get(workspaceMemberId + ":" + requestDto.fileName() + ":" + requestDto.objectKey());
        FileRequestDto.UploadPresignedUrl uploadInfo = redisStorage.getValueAsString(redisKey, FileRequestDto.UploadPresignedUrl.class);
        if (uploadInfo == null) {
            throw new FileException(FileErrorCode.INVALID_UPLOAD_REQUEST);
        }

        // Redis 키 삭제 (한 번만 사용 가능)
        redisStorage.delete(redisKey);

        validateFolder(uploadInfo.folderId(), workspaceId);

        FileType fileType = FileType.fromExtension(requestDto.fileName());

        File file = FileConverter.toFileEntityFromPresigned(
                uploadInfo.folderId(), workspaceMemberId, requestDto.fileName(),
                fileType, requestDto.objectKey(), uploadInfo.fileSize()
        );
        File savedFile = fileRepository.save(file);
        return FileConverter.toUploadResponse(savedFile);
    }

    @Override
    public FileResponseDto.Update updateFileName(Long workspaceId, Long fileId, Long workspaceMemberId, FileRequestDto.Update requestDto) {
        validateWorkspaceMembership(workspaceId, workspaceMemberId);

        File file = fileRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new FileException(FileErrorCode.FILE_NOT_FOUND));

        validateFolder(file.getFolderId(), workspaceId);

        String newName = requestDto.name();
        if (newName == null || newName.trim().isEmpty()) {
            throw new FileException(FileErrorCode.EMPTY_FILE_NAME);
        }

        // 확장자 필수 검증
        if (!newName.contains(".") || newName.lastIndexOf('.') == newName.length() - 1) {
            throw new FileException(FileErrorCode.MISSING_FILE_EXTENSION);
        }

        if (fileRepository.existsByFolderIdAndNameAndDeletedAtIsNull(file.getFolderId(), newName)
            && !file.getName().equals(newName)) {
            throw new FileException(FileErrorCode.DUPLICATE_FILE_NAME);
        }

        File updatedFile = FileConverter.toUpdatedFileEntity(file, newName);
        File savedFile = fileRepository.save(updatedFile);
        return FileConverter.toUpdateResponse(savedFile);
    }

    @Override
    public FileResponseDto.Message deleteFile(Long workspaceId, Long fileId, Long workspaceMemberId) {
        validateWorkspaceMembership(workspaceId, workspaceMemberId);

        File file = fileRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new FileException(FileErrorCode.FILE_NOT_FOUND));

        validateFolder(file.getFolderId(), workspaceId);

        File deletedFile = FileConverter.toDeletedFileEntity(file);
        fileRepository.save(deletedFile);
        return FileConverter.toMessageResponse("파일이 휴지통으로 이동되었습니다.");
    }

    @Override
    public FileResponseDto.Message restoreFile(Long workspaceId, Long fileId, Long workspaceMemberId) {
        validateWorkspaceMembership(workspaceId, workspaceMemberId);

        File file = fileRepository.findByIdAndDeletedAtIsNotNull(fileId)
                .orElseThrow(() -> new FileException(FileErrorCode.FILE_NOT_FOUND));

        // 원래 폴더가 존재하고 해당 워크스페이스에 속하는지 확인
        Long targetFolderId = file.getFolderId();
        try {
            validateFolder(file.getFolderId(), workspaceId);
        } catch (FileException e) {
            // 원래 폴더가 삭제되었거나 존재하지 않는 경우 루트 폴더로 복원
            Folder rootFolder = folderRepository.findByWorkspaceIdAndParentIdIsNull(workspaceId)
                    .orElseThrow(() -> new FileException(FileErrorCode.FORBIDDEN_ACCESS));
            targetFolderId = rootFolder.getId();
        }

        // 복원 시 파일명 중복 확인 및 고유한 이름 생성
        String uniqueName = generateUniqueFileName(targetFolderId, file.getName());

        File restoredFile = FileConverter.toRestoredFileEntity(file, uniqueName, targetFolderId);
        fileRepository.save(restoredFile);

        String message;
        if (targetFolderId.equals(file.getFolderId())) {
            // 원래 폴더로 복원
            message = uniqueName.equals(file.getName()) ?
                    "파일이 복원되었습니다." :
                    "파일이 복원되었습니다. 중복으로 인해 파일명이 '" + uniqueName + "'으로 변경되었습니다.";
        } else {
            // 루트 폴더로 복원
            message = uniqueName.equals(file.getName()) ?
                    "원래 폴더가 삭제되어 루트 폴더로 파일이 복원되었습니다." :
                    "원래 폴더가 삭제되어 루트 폴더로 파일이 복원되었습니다. 중복으로 인해 파일명이 '" + uniqueName + "'으로 변경되었습니다.";
        }

        return FileConverter.toMessageResponse(message);
    }

    @Override
    public FileResponseDto.Message hardDeleteFile(Long workspaceId, Long fileId, Long workspaceMemberId) {
        validateWorkspaceMembership(workspaceId, workspaceMemberId);

        // 삭제된 파일과 삭제되지 않은 파일 모두 조회
        File file = fileRepository.findByIdAndDeletedAtIsNull(fileId)
            .or(() -> fileRepository.findByIdAndDeletedAtIsNotNull(fileId))
            .orElseThrow(() -> new FileException(FileErrorCode.FILE_NOT_FOUND));

        validateFolder(file.getFolderId(), workspaceId);

        // S3에서 파일 삭제 (선택사항 - 필요시 구현)
        // s3Util.deleteFile(file.getObjectKey());

        // DB에서 완전 삭제
        fileRepository.delete(file);

        return FileConverter.toMessageResponse("파일이 완전히 삭제되었습니다.");
    }

    // 워크스페이스 멤버십 검증
    private void validateWorkspaceMembership(Long workspaceId, Long workspaceMemberId) {
        if (!workspaceMemberRepository.findByWorkspaceIdAndId(workspaceId, workspaceMemberId).isPresent()) {
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


    // 파일 요청 정보 유효성 검증
    private void validateFileRequest(FileRequestDto.UploadPresignedUrl request) {
        if (request.fileName() == null || request.fileName().trim().isEmpty()) {
            throw new FileException(FileErrorCode.EMPTY_FILE_NAME);
        }

        if (request.fileSize() != null && request.fileSize() > MAX_FILE_SIZE) {
            throw new FileException(FileErrorCode.FILE_SIZE_EXCEEDED);
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

    // 파일명 확장자 검증
    private void validateFileNameHasExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new FileException(FileErrorCode.EMPTY_FILE_NAME);
        }

        // 확장자 필수 검증
        if (!fileName.contains(".") || fileName.lastIndexOf('.') == fileName.length() - 1) {
            throw new FileException(FileErrorCode.MISSING_FILE_EXTENSION);
        }
    }
}
