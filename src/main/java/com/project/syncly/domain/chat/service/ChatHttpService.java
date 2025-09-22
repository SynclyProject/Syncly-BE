package com.project.syncly.domain.chat.service;


import com.project.syncly.domain.chat.dto.ChatHttpResponseDto;

public interface ChatHttpService {

    ChatHttpResponseDto.ChatResponseDto getLatestPage(Long workspaceId, Long memberId, int limit);

    ChatHttpResponseDto.ChatResponseDto getMessagesBefore(Long workspaceId, Long memberId, Long beforeSeq, int limit);

    ChatHttpResponseDto.ChatResponseDto getMessagesAfter(Long workspaceId, Long memberId, Long afterSeq, int limit);
}
