package com.project.syncly.domain.folder.repository;

import com.project.syncly.domain.folder.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    // 같은 부모 하위에 동일한 이름의 폴더가 존재하는지 확인
    boolean existsByParentIdAndName(Long parentId, String name);
}
