package com.project.syncly.domain.chat.controller;

import com.project.syncly.domain.chat.dto.ChatWebSocketRequestDto;
import com.project.syncly.domain.chat.dto.ChatWebSocketResponseDto;
import com.project.syncly.domain.chat.service.ChatWebSocketServiceImpl;
import com.project.syncly.domain.url.dto.UrlWebSocketRequestDto;
import com.project.syncly.domain.url.dto.UrlWebSocketResponseDto;
import com.project.syncly.global.apiPayload.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class ChatWebSocketController {
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatWebSocketServiceImpl chatWebSocketService;

    //채팅 메세지 전송 API
    @MessageMapping("/chat.{workspaceId}.send")
    public void sendMessage(@DestinationVariable Long workspaceId,
                            ChatWebSocketRequestDto.CreateChatRequestDto request,
                            Principal principal) {
        String userEmail = principal.getName();

        ChatWebSocketResponseDto.ChatResponseDto response = chatWebSocketService.sendMessage(workspaceId, userEmail, request);

        // "/topic/chat.{workspaceID}"를 구독한 실시간 메시지 보내기
        String destination = "/topic/chat." + workspaceId;
        messagingTemplate.convertAndSend(destination, CustomResponse.success(response));
    }
}
