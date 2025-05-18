package com.project.syncly.domain.workspace.service;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.domain.workspace.converter.WorkspaceConverter;
import com.project.syncly.domain.workspace.dto.WorkspaceResponseDto;
import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspace.entity.WorkspaceInvitation;
import com.project.syncly.domain.workspace.entity.enums.InvitationType;
import com.project.syncly.domain.workspace.entity.enums.WorkspaceType;
import com.project.syncly.domain.workspace.exception.WorkspaceErrorCode;
import com.project.syncly.domain.workspace.repository.WorkspaceInvitationRepository;
import com.project.syncly.domain.workspace.repository.WorkspaceRepository;
import com.project.syncly.domain.workspaceMember.converter.WorkspaceMemberConverter;
import com.project.syncly.domain.workspaceMember.entity.WorkspaceMember;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import com.project.syncly.global.apiPayload.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final MemberRepository memberRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final InvitationMailServiceImpl invitationMailService;


    @Value("${spring.mail.invitation.link}")
    private String invitationLinkPrefix;


    //개인 워크 스페이스 생성 API
    //거의 처음 가입하는 회원일 것이라고 예상 -> 1. 회원 조회 -> 2. 개인 워크스페이스 생성(PERSONAL로) -> 3. 워크스페이스 멤버에 개인만 저장
    @Override
    public WorkspaceResponseDto.CreateWorkspaceResponseDto createPersonalWorkspace(Long memberId) {
        //사용자 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.MEMBER_NOT_FOUND));

        //이미 개인 워크스페이스가 있는지 확인
        boolean exists = workspaceRepository.existsPersonalWorkspaceManagedBy(memberId);
        if (exists) {
            throw new CustomException(WorkspaceErrorCode.ALREADY_HAS_PERSONAL_WORKSPACE);
        }

        //개인 워크스페이스 저장
        Workspace workspace = WorkspaceConverter.toPersonalWorkspace(memberId);
        workspaceRepository.save(workspace);

        //워크스페이스 매니저로 등록
        WorkspaceMember workspaceMember = WorkspaceMemberConverter.toWorkspaceManager(member, workspace, member.getName());
        workspaceMemberRepository.save(workspaceMember);

        //응답
        return WorkspaceConverter.toWorkspaceResponse(workspace);
    }

    //팀 워크 스페이스 생성 API
    //1. 회원 조회 -> 2. 팀 워크스페이스 생성(TEAM으로) -> 3. 워크스페이스 멤버에 생성자만 저장
    @Override
    public WorkspaceResponseDto.CreateWorkspaceResponseDto createTeamWorkspace(Long memberId, String workspaceName) {
        //사용자 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.MEMBER_NOT_FOUND));

        //팀 워크스페이스 저장
        Workspace workspace = WorkspaceConverter.toTeamWorkspace(workspaceName);
        workspaceRepository.save(workspace);

        //워크스페이스 매니저로 등록
        WorkspaceMember workspaceMember = WorkspaceMemberConverter.toWorkspaceManager(member, workspace, member.getName());
        workspaceMemberRepository.save(workspaceMember);

        //응답
        return WorkspaceConverter.toWorkspaceResponse(workspace);
    }


    @Override
    public WorkspaceResponseDto.InviteWorkspaceResponseDto inviteTeamWorkspace(Long workspaceId, Long inviterId, String email) {
        // 워크 스페이스 조회
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        // 워크 스페이스가 팀 워크스페이스가 맞는지 확인
        if (workspace.getWorkspaceType() != WorkspaceType.TEAM) {
            throw new CustomException(WorkspaceErrorCode.NOT_TEAM_WORKSPACE);
        }

        // 초대한 사용자 조회 (inviter)
        Member inviter = memberRepository.findById(inviterId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.MEMBER_NOT_FOUND));

        // 초대한 사용자가 해당 워크 스페이스에 속해있는지 확인 (inviter)
        boolean isMember = workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, inviter.getId());
        if (!isMember) {
            throw new CustomException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        }

        // 초대할 사용자 조회 (invitee)
        Member invitee = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.INVITEE_NOT_FOUND));

        // 본인 초대 방지
        if (inviter.getId().equals(invitee.getId())) {
            throw new CustomException(WorkspaceErrorCode.CANNOT_INVITE_SELF);
        }

        // 워크스페이스 멤버인지 여부 확인 (이미 속해있으면 예외)
        boolean isAlreadyMember = workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, invitee.getId());
        if (isAlreadyMember) {
            throw new CustomException(WorkspaceErrorCode.ALREADY_WORKSPACE_MEMBER);
        }

        // 아직 만료되지 않은 초대가 있는지 확인
        boolean hasActiveInvite = workspaceInvitationRepository.existsByWorkspaceIdAndInviteeIdAndExpiredAtAfter(
                workspaceId, invitee.getId(), LocalDateTime.now());
        if (hasActiveInvite) {
            throw new CustomException(WorkspaceErrorCode.ALREADY_INVITED);
        }

        // 초대 토큰 생성 (invitationMailService 에서 중복 여부 확인)
        String token = invitationMailService.generateUniqueToken();

        // 이메일 전송
        String invitationLink = invitationLinkPrefix + "/api/workspaces/accept/" + token;
        invitationMailService.sendSimpleMessage(invitee.getEmail(), invitationLink); // 실제 구현 필요

        // 초대 엔티티 저장
        WorkspaceInvitation invitation = WorkspaceConverter.toInvitation(workspace, inviter, invitee, token);
        workspaceInvitationRepository.save(invitation);

        // 응답 반환
        return WorkspaceConverter.toInviteResponse(invitation, invitee.getEmail());
    }

    @Override
    public WorkspaceResponseDto.AcceptWorkspaceResponseDto acceptInvitationByToken(Long inviteeId, String token) {
        // 초대 토큰으로 초대 엔티티 조회
        WorkspaceInvitation invitation = workspaceInvitationRepository.findByToken(token)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.TOKEN_NOT_FOUND));

        // 기존 로직 그대로 재활용
        return acceptInvitation(inviteeId, invitation.getId());
    }


    @Override
    public WorkspaceResponseDto.AcceptWorkspaceResponseDto acceptInvitation(Long inviteeId, Long invitationId) {
        // invitationId로 유효한 초대인지 확인
        WorkspaceInvitation invitation = workspaceInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.INVITATION_NOT_FOUND));

        // 초대 기간이 만료되지 않았는지 혹은 PENDING 상태인지 확인
        if (invitation.getType() != InvitationType.PENDING || invitation.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new CustomException(WorkspaceErrorCode.INVITATION_EXPIRED);
        }

        // invitee가 유효한 멤버인지 확인
        Member invitee = memberRepository.findById(inviteeId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.MEMBER_NOT_FOUND));

        // workspace가 유효한 워크스페이스인지 확인
        Workspace workspace = workspaceRepository.findById(invitation.getWorkspace().getId())
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        // 이미 워크 스페이스에 참여한 멤버인지 확인
        boolean isAlreadyMember = workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspace.getId(), invitee.getId());
        if (isAlreadyMember) {
            throw new CustomException(WorkspaceErrorCode.ALREADY_WORKSPACE_MEMBER);
        }

        // invitationId에서의 invitee와 일치하는지 확인
        if (!invitation.getInvitee().getId().equals(invitee.getId())) {
            throw new CustomException(WorkspaceErrorCode.NOT_INVITEE);
        }

        // 워크스페이스 타입이 TEAM인지 확인
        if (workspace.getWorkspaceType() != WorkspaceType.TEAM) {
            throw new CustomException(WorkspaceErrorCode.CANNOT_JOIN_PERSONAL_WORKSPACE);
        }

        //새로운 workspaceMember 객체 생성, 권한은 CREW
        WorkspaceMember newMember = WorkspaceMemberConverter.toWorkspaceCrew(invitee, workspace, invitee.getName());
        workspaceMemberRepository.save(newMember);

        //초대 상태를 ACCEPTED로 변경, 응답 시간 기록
        invitation.setRespondedAt(LocalDateTime.now());
        invitation.setType(InvitationType.ACCEPTED);

        workspaceInvitationRepository.save(invitation);

        // 응답 반환
        return WorkspaceConverter.toAcceptInviteResponse(invitation);
    }

    @Override
    public WorkspaceResponseDto.RejectWorkspaceResponseDto rejectInvitation(Long inviteeId, Long invitationId) {
        // invitationId로 유효한 초대인지 확인
        WorkspaceInvitation invitation = workspaceInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.INVITATION_NOT_FOUND));

        // 초대 기간이 만료되지 않았는지 혹은 PENDING 상태인지 확인
        if (invitation.getType() != InvitationType.PENDING || invitation.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new CustomException(WorkspaceErrorCode.INVITATION_EXPIRED);
        }

        // invitee가 유효한 멤버인지 확인
        Member invitee = memberRepository.findById(inviteeId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.MEMBER_NOT_FOUND));


        //초대 상태를 rejected 로 변경, 응답 시간 기록
        invitation.setRespondedAt(LocalDateTime.now());
        invitation.setType(InvitationType.REJECTED);

        workspaceInvitationRepository.save(invitation);

        // 응답 반환
        return WorkspaceConverter.toRejectInviteResponse(invitation);
    }

    @Override
    public List<WorkspaceResponseDto.InvitationInfoDto> getInvitations(Long memberId) {
        //유효한 사용자인지 조회
        Member invitee = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.MEMBER_NOT_FOUND));

        //PENDING 상태, 만료되지 않은 모든 초대 조회
        List<WorkspaceInvitation> invitations = workspaceInvitationRepository
                .findAllByInviteeIdAndTypeAndExpiredAtAfter(invitee.getId(), InvitationType.PENDING, LocalDateTime.now());

        return WorkspaceConverter.toInvitationListResponse(invitations);
    }

}

