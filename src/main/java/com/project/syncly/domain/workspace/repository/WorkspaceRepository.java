package com.project.syncly.domain.workspace.repository;

import com.project.syncly.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    //개인 워크 스페이스 존재 여부 확인
    @Query("""
        SELECT COUNT(ws) > 0
        FROM Workspace ws
        JOIN ws.workspaceMembers wm
        WHERE wm.member.id = :memberId
          AND wm.role = 'MANAGER'
          AND ws.workspaceType = 'PERSONAL'
        """)
    boolean existsPersonalWorkspaceManagedBy(@Param("memberId") Long memberId);
}
