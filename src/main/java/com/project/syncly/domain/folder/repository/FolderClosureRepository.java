package com.project.syncly.domain.folder.repository;

import com.project.syncly.domain.folder.entity.FolderClosure;
import com.project.syncly.domain.folder.entity.FolderClosureId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderClosureRepository extends JpaRepository<FolderClosure, FolderClosureId> {
    List<FolderClosure> findByDescendantId(Long descendantId);
    boolean existsByAncestorIdAndDescendantId(Long ancestorId, Long descendantId);

    // 모든 하위 폴더 ID들 조회 (자기 자신 포함)
    @Query("SELECT fc.descendantId FROM FolderClosure fc WHERE fc.ancestorId = :ancestorId")
    List<Long> findAllDescendantIds(@Param("ancestorId") Long ancestorId);

    // 관련 경로들 삭제 (폴더와 관련된 모든 ancestor-descendant 관계 삭제)
    @Modifying
    @Query("DELETE FROM FolderClosure fc WHERE fc.ancestorId = :folderId OR fc.descendantId = :folderId")
    void deleteByAncestorIdOrDescendantId(@Param("folderId") Long folderId);

    // 루트부터 현재 폴더까지의 경로 조회 (depth 역순으로 정렬하여 루트가 첫 번째)
    @Query("SELECT f.id, f.name FROM Folder f " +
           "JOIN FolderClosure fc ON f.id = fc.ancestorId " +
           "WHERE fc.descendantId = :descendantId AND f.deletedAt IS NULL " +
           "ORDER BY fc.depth DESC")
    List<Object[]> findPathFromRoot(@Param("descendantId") Long descendantId);
}
