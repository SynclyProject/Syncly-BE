package com.project.syncly.domain.workspaceMember.repository;

import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspaceMember.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
    //이미 워크 스페이스 멤버인지 확인
    boolean existsByWorkspaceIdAndMemberId(Long workspaceId, Long memberId);
}
