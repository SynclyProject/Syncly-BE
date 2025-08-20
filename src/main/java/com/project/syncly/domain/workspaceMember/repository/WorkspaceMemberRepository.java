package com.project.syncly.domain.workspaceMember.repository;

import com.project.syncly.domain.workspace.dto.WorkspaceMemberInfoResponseDto;
import com.project.syncly.domain.workspace.dto.WorkspaceResponseDto;
import com.project.syncly.domain.workspaceMember.entity.WorkspaceMember;
import com.project.syncly.domain.workspaceMember.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
    //이미 워크 스페이스 멤버인지 확인
    boolean existsByWorkspaceIdAndMemberId(Long workspaceId, Long memberId);

    //memberId로 워크스페이스 멤버 조회
    Optional<WorkspaceMember> findByWorkspaceIdAndMemberId(Long workspaceId, Long memberId);

    //workspaceMemberId로 워크스페이스 멤버 조회
    Optional<WorkspaceMember> findByWorkspaceIdAndId(Long workspaceId, Long id);

    //워크스페이스 멤버 수 조회
    long countByWorkspaceId(Long workspaceId);

    //워크스페이스 나가기 시 위임할 멤버 조회
    @Query("""
    SELECT wm FROM WorkspaceMember wm
    JOIN wm.member m
    WHERE wm.workspace.id = :workspaceId
    AND wm.role = :role
    ORDER BY m.email ASC
    """)
    Optional<WorkspaceMember> findFirstCrewByWorkspaceIdOrderedByEmail(
            @Param("workspaceId") Long workspaceId,
            @Param("role") Role role
    );

    //워크스페이스 멤버 전체 조회, 매니저는 맨 위에 뜨고, 나머지는 이름 오름차순 조회
    @Query("""
        SELECT new com.project.syncly.domain.workspace.dto.WorkspaceMemberInfoResponseDto(
            wm.id,
            m.name,
            m.email,
            wm.role,
            wm.createdAt
        )
        FROM WorkspaceMember wm
        JOIN wm.member m
        WHERE wm.workspace.id = :workspaceId
        ORDER BY CASE WHEN wm.role = 'MANAGER' THEN 0 ELSE 1 END, m.name ASC
    """)
    List<WorkspaceMemberInfoResponseDto> findAllMembersByWorkspaceIdOrdered(@Param("workspaceId") Long workspaceId);



}
