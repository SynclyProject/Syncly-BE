package com.project.syncly.domain.url.controller;

import com.project.syncly.domain.url.dto.UrlWebSocketRequestDto;
import com.project.syncly.domain.url.dto.UrlWebSocketResponseDto;
import com.project.syncly.domain.url.service.UrlWebSocketServiceImpl;
import com.project.syncly.domain.workspace.service.WorkspaceServiceImpl;
import com.project.syncly.global.apiPayload.CustomResponse;
import com.project.syncly.global.jwt.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class UrlWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UrlWebSocketServiceImpl urlWebSocketService;

    @MessageMapping("/createTab")
    public void createUrlTab(UrlWebSocketRequestDto.CreateUrlTabRequestDto request, Principal principal) {
        String userEmail = principal.getName();

        UrlWebSocketResponseDto.CreateUrlTabResponseDto response = urlWebSocketService.createUrlTab(userEmail, request);

        //"/topic/workspace.{workspaceID}를 구독한 모든 사용자에게 실시간 탭 생성 알림
        String destination = "/topic/workspace." + request.workspaceId();
        messagingTemplate.convertAndSend(destination, CustomResponse.success(response));
    }

}
