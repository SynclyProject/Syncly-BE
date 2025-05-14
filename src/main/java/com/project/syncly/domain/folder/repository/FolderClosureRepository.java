package com.project.syncly.domain.folder.repository;

import com.project.syncly.domain.folder.entity.FolderClosure;
import com.project.syncly.domain.folder.entity.FolderClosureId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderClosureRepository extends JpaRepository<FolderClosure, FolderClosureId> {
    List<FolderClosure> findByDescendantId(Long descendantId);
    boolean existsByAncestorIdAndDescendantId(Long ancestorId, Long descendantId);
}
