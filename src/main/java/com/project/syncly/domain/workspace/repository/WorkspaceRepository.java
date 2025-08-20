package com.project.syncly.domain.workspace.repository;

import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspace.entity.enums.WorkspaceType;
import com.project.syncly.domain.workspaceMember.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
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


    //가입되어 있는 모든 워크스페이스 조회 - 생성날짜 내림차순 & 개인 워크 스페이스는 맨 위에서 조회
    @Query("""
    SELECT ws
    FROM Workspace ws
    JOIN ws.workspaceMembers wm
    WHERE wm.member.id = :memberId
    ORDER BY 
      CASE WHEN ws.workspaceType = 'PERSONAL' THEN 0 ELSE 1 END,
      ws.createdAt DESC
    """)
    List<Workspace> findAllByMemberId(@Param("memberId") Long memberId);


    //멤버 ID를 통해 개인 워크스페이스 ID 조회
    @Query("""
    SELECT w.id
    FROM Workspace w
    JOIN w.workspaceMembers wm
    JOIN wm.member m
    WHERE m.id = :memberId
      AND w.workspaceType = 'PERSONAL'
    """)
    Optional<Long> findPersonalWorkspaceIdByMemberId(@Param("memberId") Long memberId);


    //멤버 ID를 통해 개인 워크스페이스 조회
    @Query("""
    SELECT w
    FROM Workspace w
    JOIN w.workspaceMembers wm
    WHERE wm.member.id = :memberId
      AND w.workspaceType = 'PERSONAL'
    """)
    Optional<Workspace> findPersonalWorkspaceByMemberId(@Param("memberId") Long memberId);





}
