package com.project.syncly.domain.folder.repository;

import com.project.syncly.domain.folder.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    // 동일 워크스페이스 + 동일 parentId 내 존재 여부 확인
    boolean existsByWorkspaceIdAndParentIdAndName(Long workspaceId, Long parentId, String name);

    // 루트 폴더 존재 확인
    boolean existsByWorkspaceIdAndParentIdIsNull(Long workspaceId);

    // 루트 폴더 조회
    Optional<Folder> findByWorkspaceIdAndParentIdIsNull(Long workspaceId);

    // 폴더가 특정 워크스페이스에 속하는지 확인
    boolean existsByWorkspaceIdAndId(Long workspaceId, Long folderId);

    // 특정 워크스페이스의 폴더 조회
    Optional<Folder> findByIdAndWorkspaceId(Long folderId, Long workspaceId);

    // 여러 폴더 ID들을 일괄 soft delete 처리
    @Query("UPDATE Folder f SET f.deletedAt = :deletedAt WHERE f.id IN :folderIds AND f.deletedAt IS NULL")
    @Modifying
    void updateDeletedAtByIdIn(@Param("folderIds") List<Long> folderIds, @Param("deletedAt") java.time.LocalDateTime deletedAt);

    // 삭제된 폴더 조회 (워크스페이스 확인 포함)
    @Query("SELECT f FROM Folder f WHERE f.id = :folderId AND f.workspaceId = :workspaceId AND f.deletedAt IS NOT NULL")
    Optional<Folder> findDeletedByIdAndWorkspaceId(@Param("folderId") Long folderId, @Param("workspaceId") Long workspaceId);

    // 폴더 복원 (deletedAt을 null로 설정)
    @Query("UPDATE Folder f SET f.deletedAt = NULL WHERE f.id IN :folderIds")
    @Modifying
    void restoreByIdIn(@Param("folderIds") List<Long> folderIds);

    // 특정 부모 폴더 하위에서 이름이 존재하는지 확인 (삭제되지 않은 폴더만)
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Folder f " +
           "WHERE f.workspaceId = :workspaceId AND f.parentId = :parentId " +
           "AND f.name = :name AND f.deletedAt IS NULL")
    boolean existsByWorkspaceIdAndParentIdAndNameAndDeletedAtIsNull(@Param("workspaceId") Long workspaceId,
                                                                   @Param("parentId") Long parentId,
                                                                   @Param("name") String name);

    // 폴더의 부모와 이름 업데이트
    @Query("UPDATE Folder f SET f.parentId = :parentId, f.name = :name WHERE f.id = :folderId")
    @Modifying
    void updateParentIdAndName(@Param("folderId") Long folderId, @Param("parentId") Long parentId, @Param("name") String name);

    // 삭제된 하위 폴더들 조회
    @Query("SELECT f FROM Folder f WHERE f.parentId = :parentId AND f.deletedAt IS NOT NULL")
    List<Folder> findByParentIdAndDeletedAtIsNotNull(@Param("parentId") Long parentId);
}
