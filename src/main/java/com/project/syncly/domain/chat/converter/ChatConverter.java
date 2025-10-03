package com.project.syncly.domain.chat.converter;

import com.project.syncly.domain.chat.dto.ChatHttpResponseDto;
import com.project.syncly.domain.chat.dto.ChatWebSocketResponseDto;
import com.project.syncly.domain.chat.entity.ChatMessage;
import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspaceMember.entity.WorkspaceMember;

import java.util.List;


public class ChatConverter {

    public static ChatMessage toChatMessage(Workspace workspace, WorkspaceMember sender, String msgId, Long seq, String content) {
        return ChatMessage.builder()
                .workspace(workspace)
                .sender(sender)
                .msgId(msgId)
                .seq(seq)
                .content(content)
                .build();
    }

    public static ChatWebSocketResponseDto.ChatResponseDto toChatMessageResponse(ChatMessage chatMessage) {
        return ChatWebSocketResponseDto.ChatResponseDto.builder()
                .id(chatMessage.getId())
                .workspaceId(chatMessage.getWorkspace().getId())
                .senderId(chatMessage.getSender().getId())
                .senderName(chatMessage.getSender().getName())
                .senderProfileImage(chatMessage.getSender().getMember().getProfileImage())
                .msgId(chatMessage.getMsgId())
                .seq(chatMessage.getSeq())
                .content(chatMessage.getContent())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }

     // 여러 ChatMessage 엔티티를 Response DTO 리스트로 변환
    public static List<ChatWebSocketResponseDto.ChatResponseDto> toChatMessageResponseList(List<ChatMessage> messages) {
        return messages.stream()
                .map(ChatConverter::toChatMessageResponse)
                .toList();
    }

    public static ChatHttpResponseDto.ChatResponseDto toChatResponseDto(List<ChatMessage> messages, Long nextBeforeSeq, Long latestSeq) {
        return ChatHttpResponseDto.ChatResponseDto.builder()
                .items(toChatMessageResponseList(messages))
                .nextBeforeSeq(nextBeforeSeq)
                .latestSeq(latestSeq)
                .build();
    }
}
