package com.project.syncly.domain.file.converter;

import com.project.syncly.domain.file.dto.FileResponseDto;
import com.project.syncly.domain.file.entity.File;
import com.project.syncly.domain.file.enums.FileType;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

public class FileConverter {

    // 메타데이터로 File 엔티티 생성
    public static File toFileEntity(Long folderId, Long workspaceMemberId, String uniqueFileName,
                                   FileType fileType, String objectKey, MultipartFile multipartFile) {
        return File.builder()
                .folderId(folderId)
                .workspaceMemberId(workspaceMemberId)
                .name(uniqueFileName)
                .type(fileType)
                .objectKey(objectKey)
                .size(multipartFile.getSize())
                .fileUrl(null)
                .build();
    }

    // 파일 업데이트를 위한 File 엔티티 생성
    public static File toUpdatedFileEntity(File originalFile, String newName) {
        return File.builder()
                .id(originalFile.getId())
                .folderId(originalFile.getFolderId())
                .workspaceMemberId(originalFile.getWorkspaceMemberId())
                .name(newName)
                .type(originalFile.getType())
                .objectKey(originalFile.getObjectKey())
                .size(originalFile.getSize())
                .fileUrl(originalFile.getFileUrl())
                .deletedAt(originalFile.getDeletedAt())
                .build();
    }

    // 파일 삭제를 위한 File 엔티티 생성 (소프트 삭제)
    public static File toDeletedFileEntity(File originalFile) {
        return File.builder()
                .id(originalFile.getId())
                .folderId(originalFile.getFolderId())
                .workspaceMemberId(originalFile.getWorkspaceMemberId())
                .name(originalFile.getName())
                .type(originalFile.getType())
                .objectKey(originalFile.getObjectKey())
                .size(originalFile.getSize())
                .fileUrl(originalFile.getFileUrl())
                .deletedAt(LocalDateTime.now())
                .build();
    }

    // 파일 복원을 위한 File 엔티티 생성
    public static File toRestoredFileEntity(File originalFile) {
        return File.builder()
                .id(originalFile.getId())
                .folderId(originalFile.getFolderId())
                .workspaceMemberId(originalFile.getWorkspaceMemberId())
                .name(originalFile.getName())
                .type(originalFile.getType())
                .objectKey(originalFile.getObjectKey())
                .size(originalFile.getSize())
                .fileUrl(originalFile.getFileUrl())
                .deletedAt(null)
                .build();
    }

    // 파일 복원을 위한 File 엔티티 생성 (고유한 이름으로)
    public static File toRestoredFileEntity(File originalFile, String uniqueName) {
        return File.builder()
                .id(originalFile.getId())
                .folderId(originalFile.getFolderId())
                .workspaceMemberId(originalFile.getWorkspaceMemberId())
                .name(uniqueName)
                .type(originalFile.getType())
                .objectKey(originalFile.getObjectKey())
                .size(originalFile.getSize())
                .fileUrl(originalFile.getFileUrl())
                .deletedAt(null)
                .build();
    }

    // 파일 복원을 위한 File 엔티티 생성 (고유한 이름 및 폴더ID로)
    public static File toRestoredFileEntity(File originalFile, String uniqueName, Long targetFolderId) {
        return File.builder()
                .id(originalFile.getId())
                .folderId(targetFolderId)
                .workspaceMemberId(originalFile.getWorkspaceMemberId())
                .name(uniqueName)
                .type(originalFile.getType())
                .objectKey(originalFile.getObjectKey())
                .size(originalFile.getSize())
                .fileUrl(originalFile.getFileUrl())
                .deletedAt(null)
                .build();
    }

    // File 엔티티를 Upload 응답 DTO로 변환
    public static FileResponseDto.Upload toUploadResponse(File file) {
        return new FileResponseDto.Upload(
                file.getId(),
                file.getName(),
                file.getType().getKey(),
                file.getSize(),
                file.getCreatedAt()
        );
    }

    // File 엔티티를 Update 응답 DTO로 변환
    public static FileResponseDto.Update toUpdateResponse(File file) {
        return new FileResponseDto.Update(
                file.getId(),
                file.getName(),
                file.getUpdatedAt()
        );
    }

    // Presigned URL로 업로드된 파일을 위한 File 엔티티 생성
    public static File toFileEntityFromPresigned(Long folderId, Long workspaceMemberId, String fileName,
                                               FileType fileType, String objectKey, Long fileSize) {
        return File.builder()
                .folderId(folderId)
                .workspaceMemberId(workspaceMemberId)
                .name(fileName)
                .type(fileType)
                .objectKey(objectKey)
                .size(fileSize != null ? fileSize : 0L)
                .fileUrl(null)
                .build();
    }

    // 메시지 응답 DTO 생성
    public static FileResponseDto.Message toMessageResponse(String message) {
        return new FileResponseDto.Message(message);
    }
}