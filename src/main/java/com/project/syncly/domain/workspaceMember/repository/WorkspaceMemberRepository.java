package com.project.syncly.domain.workspaceMember.repository;

import com.project.syncly.domain.workspaceMember.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
    //이미 워크 스페이스 멤버인지 확인
    boolean existsByWorkspaceIdAndMemberId(Long workspaceId, Long memberId);

    //워크스페이스 멤버 조회
    Optional<WorkspaceMember> findByWorkspaceIdAndMemberId(Long workspaceId, Long memberId);
}
