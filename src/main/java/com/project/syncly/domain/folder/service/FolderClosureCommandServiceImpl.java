package com.project.syncly.domain.folder.service;

import com.project.syncly.domain.folder.converter.FolderConverter;
import com.project.syncly.domain.folder.entity.FolderClosure;
import com.project.syncly.domain.folder.repository.FolderClosureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FolderClosureCommandServiceImpl implements FolderClosureCommandService {

    private final FolderClosureRepository folderClosureRepository;

    @Override
    public void updateOnCreate(Long parentId, Long newFolderId) {

        // [1] 자기 자신 → 자기 자신 (depth = 0) 삽입
        // 중복 삽입 방지 위해 이미 존재하는지 확인
        if (folderClosureRepository.existsByAncestorIdAndDescendantId(newFolderId, newFolderId)) {
            folderClosureRepository.save(FolderConverter.toFolderClosure(newFolderId, newFolderId, 0));
        }

        // [2] 모든 조상 → 자기 자신 삽입
        if (parentId != null) { // 상위 폴더가 있다면
            // 상위 폴더의 모든 조상을 가져옴
            List<FolderClosure> ancestors = folderClosureRepository.findByDescendantId(parentId);
            // 각 조상에 대해 ' 기존 depth + 1 ' 을 거리로 추가
            for (FolderClosure ancestor : ancestors) {
                Long ancestorId = ancestor.getAncestorId();
                int depth = ancestor.getDepth() + 1;
                // 조상 -> 새 폴더 관계가 이미 없다면 삽입
                if (folderClosureRepository.existsByAncestorIdAndDescendantId(ancestorId, newFolderId)) {
                    folderClosureRepository.save(FolderConverter.toFolderClosure(ancestorId, newFolderId, depth));
                }
            }
        }
    }
}
