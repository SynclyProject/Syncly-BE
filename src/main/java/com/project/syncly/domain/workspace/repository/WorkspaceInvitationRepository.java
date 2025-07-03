package com.project.syncly.domain.workspace.repository;

import com.project.syncly.domain.workspace.entity.WorkspaceInvitation;
import com.project.syncly.domain.workspace.entity.enums.InvitationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {
    //이미 보댄 초대가 있는지 확인
    boolean existsByWorkspaceIdAndInviteeIdAndExpiredAtAfter(Long workspaceId, Long inviteeId, LocalDateTime now);

    //이미 보댄 초대가 있다면, 조회
    Optional<WorkspaceInvitation> findByWorkspaceIdAndInviteeIdAndExpiredAtAfter(Long workspaceId, Long inviteeId, LocalDateTime now);

    //토큰으로 초대 조회
    Optional<WorkspaceInvitation> findByToken(String token);

    //사용자에게 온 모든 초대 내역 조회
    List<WorkspaceInvitation> findAllByInviteeIdAndTypeAndExpiredAtAfter(Long inviteeId, InvitationType type, LocalDateTime now
    );


}
