package com.project.syncly.domain.folder.repository;

import com.project.syncly.domain.folder.entity.FolderClosure;
import com.project.syncly.domain.folder.entity.FolderClosureId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderClosureRepository extends JpaRepository<FolderClosure, FolderClosureId> {
    // 특정 폴더의 조상 목록 조회
    List<FolderClosure> findByDescendantId(Long descendantId);

    // 중복 삽입 방지 위해 ancestorId, descendantId 쌍이 이미 존재하는지 확인
    boolean existsByAncestorIdAndDescendantId(Long ancestorId, Long descendantId);
}
