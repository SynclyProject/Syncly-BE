package com.project.syncly.domain.folder.repository;

import com.project.syncly.domain.folder.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    boolean existsByParentIdAndName(Long parentId, String name);
}
