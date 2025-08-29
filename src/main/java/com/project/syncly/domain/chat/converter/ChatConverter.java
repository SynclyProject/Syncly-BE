package com.project.syncly.domain.chat.converter;

import com.project.syncly.domain.chat.dto.ChatWebSocketResponseDto;
import com.project.syncly.domain.chat.entity.ChatMessage;
import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspaceMember.entity.WorkspaceMember;


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
                .msgId(chatMessage.getMsgId())
                .seq(chatMessage.getSeq())
                .content(chatMessage.getContent())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }
}
