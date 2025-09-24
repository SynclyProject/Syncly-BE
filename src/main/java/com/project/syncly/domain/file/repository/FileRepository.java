package com.project.syncly.domain.file.repository;

import com.project.syncly.domain.file.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {

    // 폴더 내에 동일한 이름의 파일이 존재하는지 확인 (삭제되지 않은 파일만)
    boolean existsByFolderIdAndNameAndDeletedAtIsNull(Long folderId, String name);

    // 폴더 ID와 파일명으로 파일 조회 (삭제되지 않은 파일만)
    Optional<File> findByFolderIdAndNameAndDeletedAtIsNull(Long folderId, String name);

    // 폴더 ID와 파일명으로 파일 조회 (삭제여부 상관없이)
    Optional<File> findByFolderIdAndName(Long folderId, String name);

    // 워크스페이스 멤버 ID로 업로드한 파일 목록 조회 (삭제되지 않은 파일만)
    List<File> findByWorkspaceMemberIdAndDeletedAtIsNull(Long workspaceMemberId);

    // 폴더 ID로 파일 목록 조회 (삭제되지 않은 파일만)
    List<File> findByFolderIdAndDeletedAtIsNull(Long folderId);

    // 워크스페이스 ID로 모든 파일 조회 (폴더를 통해 연결)
    @Query("SELECT f FROM File f " +
           "JOIN Folder folder ON f.folderId = folder.id " +
           "WHERE folder.workspaceId = :workspaceId AND f.deletedAt IS NULL")
    List<File> findAllByWorkspaceIdThroughFolder(@Param("workspaceId") Long workspaceId);

    // 파일 ID로 조회 (삭제되지 않은 파일만)
    @Query("SELECT f FROM File f WHERE f.id = :fileId AND f.deletedAt IS NULL")
    Optional<File> findByIdAndDeletedAtIsNull(@Param("fileId") Long fileId);

    // 파일 ID로 조회 (삭제된 파일만)
    @Query("SELECT f FROM File f WHERE f.id = :fileId AND f.deletedAt IS NOT NULL")
    Optional<File> findByIdAndDeletedAtIsNotNull(@Param("fileId") Long fileId);

    // 워크스페이스의 휴지통 파일 조회
    @Query("SELECT f FROM File f " +
           "JOIN Folder folder ON f.folderId = folder.id " +
           "WHERE folder.workspaceId = :workspaceId AND f.deletedAt IS NOT NULL")
    List<File> findTrashFilesByWorkspaceId(@Param("workspaceId") Long workspaceId);
}