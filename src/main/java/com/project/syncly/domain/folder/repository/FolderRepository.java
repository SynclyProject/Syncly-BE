package com.project.syncly.domain.folder.repository;

import com.project.syncly.domain.folder.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
