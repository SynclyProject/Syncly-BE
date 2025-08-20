package com.project.syncly.domain.url.service;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.domain.url.converter.UrlItemConverter;
import com.project.syncly.domain.url.converter.UrlTabConverter;
import com.project.syncly.domain.url.dto.UrlHttpRequestDto;
import com.project.syncly.domain.url.dto.UrlHttpResponseDto;
import com.project.syncly.domain.url.entity.UrlItem;
import com.project.syncly.domain.url.entity.UrlTab;
import com.project.syncly.domain.url.exception.UrlErrorCode;
import com.project.syncly.domain.url.exception.UrlException;
import com.project.syncly.domain.url.repository.UrlItemRepository;
import com.project.syncly.domain.url.repository.UrlTabRepository;
import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspace.entity.enums.WorkspaceType;
import com.project.syncly.domain.workspace.exception.WorkspaceErrorCode;
import com.project.syncly.domain.workspace.exception.WorkspaceException;
import com.project.syncly.domain.workspace.repository.WorkspaceRepository;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import com.project.syncly.global.apiPayload.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
public class UrlHttpServiceImpl implements UrlHttpService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UrlTabRepository urlTabRepository;
    private final UrlItemRepository urlItemRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
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

    @Override
    public UrlHttpResponseDto.CreateUrlTabResponseDto createUrlTab(Long memberId, UrlHttpRequestDto.CreateUrlTabRequestDto request) {
        // 개인 워크스페이스 조회
        Workspace workspace = workspaceRepository.findPersonalWorkspaceByMemberId(memberId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));


        // URL 탭 생성 및 저장
        UrlTab urlTab = UrlTabConverter.toUrlTab(workspace, request.urlTabName());
        urlTabRepository.save(urlTab);

        //반환
        return UrlTabConverter.toUrlTabHttpResponse(urlTab);
    }

    @Override
    public UrlHttpResponseDto.DeleteUrlTabResponseDto deleteUrlTab(Long memberId, Long tabId) {
        // 개인 워크스페이스 ID 조회
        Long workspaceId = workspaceRepository.findPersonalWorkspaceIdByMemberId(memberId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));


        // URL 탭 조회
        UrlTab urlTab = urlTabRepository.findById(tabId)
                .orElseThrow(() -> new UrlException(UrlErrorCode.URL_TAB_NOT_FOUND));

        if (!urlTab.getWorkspace().getId().equals(workspaceId)) {
            throw new UrlException(UrlErrorCode.URL_TAB_NOT_BELONG_TO_WORKSPACE);
        }

        //삭제
        urlTabRepository.delete(urlTab);

        //반환
        return UrlTabConverter.toDeleteUrlTabHttpResponse(tabId, workspaceId);
    }

    @Override
    public UrlHttpResponseDto.UpdateUrlTabNameResponseDto updateUrlTabName(Long memberId, Long tabId, UrlHttpRequestDto.UpdateUrlTabNameRequestDto request) {
        // 개인 워크스페이스 ID 조회
        Long workspaceId = workspaceRepository.findPersonalWorkspaceIdByMemberId(memberId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        // URL 탭 조회
        UrlTab urlTab = urlTabRepository.findById(tabId)
                .orElseThrow(() -> new UrlException(UrlErrorCode.URL_TAB_NOT_FOUND));

        if (!urlTab.getWorkspace().getId().equals(workspaceId)) {
            throw new UrlException(UrlErrorCode.URL_TAB_NOT_BELONG_TO_WORKSPACE);
        }

        urlTab.updateTabName(request.newUrlTabName());
        urlTabRepository.save(urlTab);

        return UrlTabConverter.toUpdateUrlTabNameHttpResponse(urlTab);
    }

    @Override
    public UrlHttpResponseDto.AddUrlItemResponseDto addUrlItem(Long memberId, Long tabId, UrlHttpRequestDto.AddUrlItemRequestDto request) {
        // URL 탭 조회
        UrlTab urlTab = urlTabRepository.findById(tabId)
                .orElseThrow(() -> new UrlException(UrlErrorCode.URL_TAB_NOT_FOUND));

        Workspace workspace = urlTab.getWorkspace();
        Long workspaceId = workspace.getId();

        // 개인 워크스페이스인지 확인
        if (workspace.getWorkspaceType() != WorkspaceType.PERSONAL) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_PERSONAL_WORKSPACE);
        }

        // 워크스페이스 멤버 확인
        workspaceMemberRepository.findByWorkspaceIdAndMemberId(workspaceId, memberId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER));

        // URL 유효성 검사
        try {
            new URL(request.url());  // java.net.URL을 통해 URL 형태 유효성 검증
        } catch (MalformedURLException e) {
            throw new UrlException(UrlErrorCode.INVALID_URL_FORMAT);
        }

        // URL 아이템 생성 및 저장
        UrlItem urlItem = UrlItemConverter.toUrlItem(urlTab, request.url());
        urlItemRepository.save(urlItem);

        // 응답
        return UrlItemConverter.toAddUrlItemHttpResponse(urlItem);
    }

    @Override
    public UrlHttpResponseDto.DeleteUrlItemResponseDto deleteUrlItem(Long memberId, Long tabId, Long itemId) {
        // URL 탭 조회
        UrlTab urlTab = urlTabRepository.findById(tabId)
                .orElseThrow(() -> new UrlException(UrlErrorCode.URL_TAB_NOT_FOUND));

        Workspace workspace = urlTab.getWorkspace();
        Long workspaceId = workspace.getId();

        // 개인 워크스페이스인지 확인
        if (workspace.getWorkspaceType() != WorkspaceType.PERSONAL) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_PERSONAL_WORKSPACE);
        }

        // 워크스페이스 멤버 확인
        workspaceMemberRepository.findByWorkspaceIdAndMemberId(workspaceId, memberId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER));

        // URL 아이템 조회
        UrlItem urlItem = urlItemRepository.findById(itemId)
                .orElseThrow(() -> new UrlException(UrlErrorCode.URL_ITEM_NOT_FOUND));

        // 탭 ID 불일치 확인
        if (!urlItem.getUrlTab().getId().equals(tabId)) {
            throw new UrlException(UrlErrorCode.URL_ITEM_NOT_BELONG_TO_TAB);
        }

        // 삭제
        urlItemRepository.delete(urlItem);

        // 응답
        return UrlItemConverter.toDeleteUrlItemHttpResponse(urlItem);
    }





}
