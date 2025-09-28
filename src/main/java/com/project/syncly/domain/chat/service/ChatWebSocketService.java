package com.project.syncly.domain.chat.service;

import com.project.syncly.domain.chat.dto.ChatWebSocketRequestDto;
import com.project.syncly.domain.chat.dto.ChatWebSocketResponseDto;
import com.project.syncly.domain.url.dto.UrlWebSocketRequestDto;
import com.project.syncly.domain.url.dto.UrlWebSocketResponseDto;

public interface ChatWebSocketService {
    public ChatWebSocketResponseDto.ChatResponseDto sendMessage(Long workspaceId, String userEmail, ChatWebSocketRequestDto.CreateChatRequestDto request);
}
