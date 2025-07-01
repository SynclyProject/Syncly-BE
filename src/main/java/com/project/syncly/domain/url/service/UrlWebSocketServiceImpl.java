package com.project.syncly.domain.url.service;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.exception.MemberErrorCode;
import com.project.syncly.domain.member.exception.MemberException;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.domain.url.converter.UrlTabConverter;
import com.project.syncly.domain.url.dto.UrlWebSocketRequestDto;
import com.project.syncly.domain.url.dto.UrlWebSocketResponseDto;
import com.project.syncly.domain.url.entity.UrlTab;
import com.project.syncly.domain.url.exception.UrlErrorCode;
import com.project.syncly.domain.url.exception.UrlException;
import com.project.syncly.domain.url.repository.UrlTabRepository;
import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspace.entity.enums.WorkspaceType;
import com.project.syncly.domain.workspace.exception.WorkspaceErrorCode;
import com.project.syncly.domain.workspace.repository.WorkspaceRepository;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import com.project.syncly.global.apiPayload.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;

@Service
@RequiredArgsConstructor
public class UrlWebSocketServiceImpl implements UrlWebSocketService {

    private final WorkspaceRepository workspaceRepository;
    private final MemberRepository memberRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UrlTabRepository urlTabRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public UrlWebSocketResponseDto.CreateUrlTabResponseDto createUrlTab(String userEmail, UrlWebSocketRequestDto.CreateUrlTabRequestDto request) {

        //연결 확인
        boolean isConnected = Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(RedisKeyPrefix.WS_ONLINE_USERS.get(), userEmail));
        if (!isConnected) {
            throw new UrlException(UrlErrorCode.USER_NOT_CONNECTED);
        }


        // 이메일로 Member 조회
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        Long memberId = member.getId(); // 실제 ID 추출

        // 워크스페이스 조회
        Long workspaceId = request.workspaceId();

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        // 팀 워크스페이스인지 확인
        if (workspace.getWorkspaceType() != WorkspaceType.TEAM) {
            throw new CustomException(WorkspaceErrorCode.NOT_TEAM_WORKSPACE);
        }

        // 워크스페이스 멤버 조회
        workspaceMemberRepository.findByWorkspaceIdAndMemberId(workspaceId, memberId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER));


        // URL 탭 생성 & 저장
        UrlTab urlTab = UrlTabConverter.toUrlTab(workspace, request.urlTabName());
        urlTabRepository.save(urlTab);


        //응답 반환
        return UrlTabConverter.toUrlTabResponse(urlTab);
    }

    @Override
    public UrlWebSocketResponseDto.DeleteUrlTabResponseDto deleteUrlTab(String userEmail, UrlWebSocketRequestDto.DeleteUrlTabRequestDto request) {
        // 연결 확인
        boolean isConnected = Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(RedisKeyPrefix.WS_ONLINE_USERS.get(), userEmail));
        if (!isConnected) {
            throw new UrlException(UrlErrorCode.USER_NOT_CONNECTED);
        }

        // 사용자 조회
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        Long memberId = member.getId();
        Long workspaceId = request.workspaceId();
        Long urlTabId = request.urlTabId();

        // 워크스페이스 조회
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        // 팀 워크스페이스 여부 확인
        if (workspace.getWorkspaceType() != WorkspaceType.TEAM) {
            throw new CustomException(WorkspaceErrorCode.NOT_TEAM_WORKSPACE);
        }

        // 워크스페이스 멤버 확인
        workspaceMemberRepository.findByWorkspaceIdAndMemberId(workspaceId, memberId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER));

        // URL 탭 존재 여부 확인
        UrlTab urlTab = urlTabRepository.findById(urlTabId)
                .orElseThrow(() -> new UrlException(UrlErrorCode.URL_TAB_NOT_FOUND));

        // 탭의 워크스페이스 소속 확인
        if (!urlTab.getWorkspace().getId().equals(workspaceId)) {
            throw new UrlException(UrlErrorCode.URL_TAB_NOT_BELONG_TO_WORKSPACE);
        }

        // 삭제
        urlTabRepository.delete(urlTab);

        // 반환
        return UrlTabConverter.toDeleteUrlTabResponse(urlTabId, workspaceId);
    }

    @Override
    public UrlWebSocketResponseDto.UpdateUrlTabNameResponseDto updateUrlTabName(String userEmail, UrlWebSocketRequestDto.UpdateUrlTabNameRequestDto request) {
        // 연결 확인
        boolean isConnected = Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(RedisKeyPrefix.WS_ONLINE_USERS.get(), userEmail));
        if (!isConnected) {
            throw new UrlException(UrlErrorCode.USER_NOT_CONNECTED);
        }

        // 사용자 조회
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        Long memberId = member.getId();
        Long workspaceId = request.workspaceId();
        Long urlTabId = request.urlTabId();

        // 워크스페이스 조회
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        // 팀 워크스페이스 여부 확인
        if (workspace.getWorkspaceType() != WorkspaceType.TEAM) {
            throw new CustomException(WorkspaceErrorCode.NOT_TEAM_WORKSPACE);
        }

        // 워크스페이스 멤버 확인
        workspaceMemberRepository.findByWorkspaceIdAndMemberId(workspaceId, memberId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER));

        // URL 탭 조회
        UrlTab urlTab = urlTabRepository.findById(urlTabId)
                .orElseThrow(() -> new UrlException(UrlErrorCode.URL_TAB_NOT_FOUND));

        // 워크스페이스 일치 확인
        if (!urlTab.getWorkspace().getId().equals(workspaceId)) {
            throw new UrlException(UrlErrorCode.URL_TAB_NOT_BELONG_TO_WORKSPACE);
        }

        // 이름 변경
        urlTab.updateTabName(request.newUrlTabName());

        // 저장
        urlTabRepository.save(urlTab);

        // 응답 변환
        return UrlTabConverter.toUpdateUrlTabNameResponse(urlTab);
    }


}
