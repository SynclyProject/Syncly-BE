package com.project.syncly.domain.workspace.service;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.domain.workspace.converter.WorkspaceConverter;
import com.project.syncly.domain.workspace.dto.WorkspaceMemberInfoResponseDto;
import com.project.syncly.domain.workspace.dto.WorkspaceResponseDto;
import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspace.exception.WorkspaceErrorCode;
import com.project.syncly.domain.workspace.repository.WorkspaceRepository;
import com.project.syncly.domain.workspaceMember.converter.WorkspaceMemberConverter;
import com.project.syncly.domain.workspaceMember.entity.WorkspaceMember;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import com.project.syncly.global.apiPayload.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

public interface WorkspaceService {
    public WorkspaceResponseDto.CreateWorkspaceResponseDto createPersonalWorkspace(Long memberId);
    public WorkspaceResponseDto.CreateWorkspaceResponseDto createTeamWorkspace(Long memberId, String workspaceName);
    public WorkspaceResponseDto.InviteWorkspaceResponseDto inviteTeamWorkspace(Long workspaceId, Long inviterId, String email);
    public WorkspaceResponseDto.AcceptWorkspaceResponseDto acceptInvitationByToken(Long inviteeId, String token);
    public WorkspaceResponseDto.AcceptWorkspaceResponseDto acceptInvitation(Long inviteeId, Long invitationId);
    public WorkspaceResponseDto.RejectWorkspaceResponseDto rejectInvitation(Long inviteeId, Long invitationId);
    public List<WorkspaceResponseDto.InvitationInfoDto> getInvitations(Long memberId);
    public WorkspaceResponseDto.RenameWorkspaceResponseDto renameTeamWorkspace(Long workspaceId, Long memberId, String newName);
    public WorkspaceResponseDto.LeaveWorkspaceResponseDto leaveWorkspace(Long workspaceId, Long memberId);
    public WorkspaceResponseDto.KickMemberResponseDto kickMember(Long workspaceId, Long memberId, Long targetMemberId);
    public List<WorkspaceResponseDto.MyWorkspaceResponseDto> getMyWorkspaces(Long memberId);
    public List<WorkspaceMemberInfoResponseDto> getWorkspaceMembers(Long workspaceId);
}

