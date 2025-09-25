package com.project.syncly.domain.chat.service;

import com.project.syncly.domain.chat.dto.ChatHttpResponseDto;
import com.project.syncly.domain.chat.entity.ChatMessage;
import com.project.syncly.domain.chat.exception.ChatErrorCode;
import com.project.syncly.domain.chat.repository.ChatMessageRepository;
import com.project.syncly.domain.chat.converter.ChatConverter;
import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspace.entity.enums.WorkspaceType;
import com.project.syncly.domain.workspace.exception.WorkspaceErrorCode;
import com.project.syncly.domain.workspace.repository.WorkspaceRepository;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import com.project.syncly.global.apiPayload.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatHttpServiceImpl implements ChatHttpService {

    private final ChatMessageRepository chatMessageRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final MemberRepository memberRepository;

    @Override
    public ChatHttpResponseDto.ChatResponseDto getLatestPage(Long workspaceId, Long memberId, int limit) {
        // 멤버 존재 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ChatErrorCode.MEMBER_NOT_FOUND));

        // 워크스페이스 존재 여부 확인
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new CustomException(ChatErrorCode.WORKSPACE_NOT_FOUND));

        // 워크스페이스 멤버 여부 확인
        workspaceMemberRepository.findByWorkspaceIdAndMemberId(workspaceId, member.getId())
                .orElseThrow(() -> new CustomException(ChatErrorCode.NOT_WORKSPACE_MEMBER));

        // 팀 워크스페이스인지 확인
        if (workspace.getWorkspaceType() != WorkspaceType.TEAM) {
            throw new CustomException(ChatErrorCode.NOT_TEAM_WORKSPACE);
        }

        // 최신 limit개 메시지
        List<ChatMessage> messages = chatMessageRepository.findLatest(workspaceId, PageRequest.of(0, limit));
        Collections.reverse(messages);

        //불러온 메세지 중 가장 예전 메시지의 seq
        Long nextBeforeSeq = messages.isEmpty() ? null : messages.get(0).getSeq();

        //가장 최신의 메세지 seq
        Long latestSeq = chatMessageRepository.findLatestSeq(workspaceId);

        return ChatConverter.toChatResponseDto(messages, nextBeforeSeq, latestSeq);
    }

    @Override
    public ChatHttpResponseDto.ChatResponseDto getMessagesBefore(Long workspaceId, Long memberId, Long beforeSeq, int limit) {
        // 멤버 존재 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.MEMBER_NOT_FOUND));

        // 워크스페이스 존재 여부 확인
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new CustomException(ChatErrorCode.WORKSPACE_NOT_FOUND));

        // 워크스페이스 멤버 여부 확인
        workspaceMemberRepository.findByWorkspaceIdAndMemberId(workspaceId, member.getId())
                .orElseThrow(() -> new CustomException(ChatErrorCode.NOT_WORKSPACE_MEMBER));

        // 팀 워크스페이스인지 확인
        if (workspace.getWorkspaceType() != WorkspaceType.TEAM) {
            throw new CustomException(ChatErrorCode.NOT_TEAM_WORKSPACE);
        }

        List<ChatMessage> messages = chatMessageRepository.findBefore(workspaceId, beforeSeq, PageRequest.of(0, limit));
        Collections.reverse(messages);

        Long nextBeforeSeq = messages.isEmpty() ? null : messages.get(0).getSeq();
        Long latestSeq = chatMessageRepository.findLatestSeq(workspaceId);

        return ChatConverter.toChatResponseDto(messages, nextBeforeSeq, latestSeq);
    }

    @Override
    public ChatHttpResponseDto.ChatResponseDto getMessagesAfter(Long workspaceId, Long memberId, Long afterSeq, int limit) {
        // 멤버 존재 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.MEMBER_NOT_FOUND));

        // 워크스페이스 존재 여부 확인
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new CustomException(ChatErrorCode.WORKSPACE_NOT_FOUND));

        // 워크스페이스 멤버 여부 확인
        workspaceMemberRepository.findByWorkspaceIdAndMemberId(workspaceId, member.getId())
                .orElseThrow(() -> new CustomException(ChatErrorCode.NOT_WORKSPACE_MEMBER));

        // 팀 워크스페이스인지 확인
        if (workspace.getWorkspaceType() != WorkspaceType.TEAM) {
            throw new CustomException(ChatErrorCode.NOT_TEAM_WORKSPACE);
        }

        List<ChatMessage> messages = chatMessageRepository.findAfter(workspaceId, afterSeq, PageRequest.of(0, limit));

        Long nextBeforeSeq = messages.isEmpty() ? null : messages.get(0).getSeq();
        Long latestSeq = chatMessageRepository.findLatestSeq(workspaceId);

        return ChatConverter.toChatResponseDto(messages, nextBeforeSeq, latestSeq);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getMyWorkspaceMemberId(Long workspaceId, Long memberId) {
        // 멤버 존재 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ChatErrorCode.MEMBER_NOT_FOUND));

        // 워크스페이스 존재 여부 확인
        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new CustomException(ChatErrorCode.WORKSPACE_NOT_FOUND));

        // 워크스페이스 멤버 조회
        return workspaceMemberRepository.findByWorkspaceIdAndMemberId(workspaceId, member.getId())
                .orElseThrow(() -> new CustomException(ChatErrorCode.NOT_WORKSPACE_MEMBER))
                .getId();  // PK(workspaceMemberId) 반환
    }
}
