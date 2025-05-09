package com.project.syncly.domain.workspace.service;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.domain.workspace.converter.WorkspaceConverter;
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

@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final MemberRepository memberRepository;


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

}

