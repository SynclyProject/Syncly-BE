package com.project.syncly.domain.url.service;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.domain.url.converter.UrlItemConverter;
import com.project.syncly.domain.url.dto.UrlHttpResponseDto;
import com.project.syncly.domain.url.entity.UrlTab;
import com.project.syncly.domain.url.repository.UrlTabRepository;
import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspace.exception.WorkspaceErrorCode;
import com.project.syncly.domain.workspace.repository.WorkspaceRepository;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import com.project.syncly.global.apiPayload.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class UrlHttpServiceImpl implements UrlHttpService {

    private final WorkspaceRepository workspaceRepository;
    private final MemberRepository memberRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UrlTabRepository urlTabRepository;

    @Override
    public UrlHttpResponseDto.TabsWithUrlsResponseDto getTabsWithUrls(Long workspaceId, Long memberId) {
        // 멤버 존재 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.MEMBER_NOT_FOUND));

        // 워크스페이스 유효성 검사
        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        // 워크스페이스 멤버 여부 확인
        workspaceMemberRepository.findByWorkspaceIdAndMemberId(workspaceId, member.getId())
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER));

        // URL 탭 + URL 아이템 조회
        List<UrlTab> urlTabs = urlTabRepository.findAllWithUrlItemsByWorkspaceId(workspaceId);

        return UrlItemConverter.toTabsWithUrlsResponse(workspaceId, urlTabs);
    }


}
