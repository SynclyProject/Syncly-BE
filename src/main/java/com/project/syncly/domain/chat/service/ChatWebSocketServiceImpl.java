package com.project.syncly.domain.chat.service;

import com.project.syncly.domain.chat.converter.ChatConverter;
import com.project.syncly.domain.chat.dto.ChatWebSocketRequestDto;
import com.project.syncly.domain.chat.dto.ChatWebSocketResponseDto;
import com.project.syncly.domain.chat.entity.ChatMessage;
import com.project.syncly.domain.chat.repository.ChatMessageRepository;
import com.project.syncly.domain.chat.repository.WorkspaceSeqRepository;
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
import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspace.entity.enums.WorkspaceType;
import com.project.syncly.domain.workspace.exception.WorkspaceErrorCode;
import com.project.syncly.domain.workspace.repository.WorkspaceRepository;
import com.project.syncly.domain.workspaceMember.entity.WorkspaceMember;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import com.project.syncly.global.apiPayload.exception.CustomException;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatWebSocketServiceImpl implements ChatWebSocketService {

    private final RedisTemplate<String, String> redisTemplate;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final MemberRepository memberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ChatMessageRepository chatRepository;
    private final SeqAllocator seqAllocator;


    @Override
    public ChatWebSocketResponseDto.ChatResponseDto sendMessage(Long workspaceId, String userEmail, ChatWebSocketRequestDto.CreateChatRequestDto request) {
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
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        // 팀 워크스페이스인지 확인
        if (workspace.getWorkspaceType() != WorkspaceType.TEAM) {
            throw new CustomException(WorkspaceErrorCode.NOT_TEAM_WORKSPACE);
        }

        // 워크스페이스 멤버 조회
        WorkspaceMember sender = workspaceMemberRepository.findByWorkspaceIdAndMemberId(workspaceId, memberId)
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER));

        // 멱등 처리 - 이미 존재하면 그대로 반환(네트워크 오류로 인해 같은 메시지가 여러번 재전송 되었을 경우)
        var dup = chatRepository.findByWorkspaceIdAndMsgId(workspaceId, request.msgId());
        if (dup.isPresent()) return ChatConverter.toChatMessageResponse(dup.get());

        // seq 배정
        long seq = seqAllocator.nextSeq(workspaceId);

        // 저장
        ChatMessage chatMessage = ChatConverter.toChatMessage(workspace, sender, request.msgId(), seq, request.content());
        chatRepository.save(chatMessage);

        ChatMessage loadedMessage = chatRepository.findByIdWithSenderAndMember(chatMessage.getId())
                .orElseThrow(() -> new CustomException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        return ChatConverter.toChatMessageResponse(loadedMessage);
    }
}
