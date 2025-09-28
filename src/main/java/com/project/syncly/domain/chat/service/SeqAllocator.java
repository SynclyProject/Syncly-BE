package com.project.syncly.domain.chat.service;

import com.project.syncly.domain.chat.entity.WorkspaceSeq;
import com.project.syncly.domain.chat.repository.WorkspaceSeqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SeqAllocator {

    private final WorkspaceSeqRepository seqRepository;

    @Transactional
    public long nextSeq(Long workspaceId) {
        int updated = seqRepository.bumpAndSetLastInsertId(workspaceId);
        if (updated == 0) {
            // workspace_seq 행이 없을 때 초기화
            seqRepository.save(new WorkspaceSeq(workspaceId, 0L));
            seqRepository.bumpAndSetLastInsertId(workspaceId);
        }
        return seqRepository.fetchLastInsertId();
    }
}