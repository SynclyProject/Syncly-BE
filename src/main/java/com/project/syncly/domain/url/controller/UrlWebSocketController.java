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

    //URL 탭 생성 API
    @MessageMapping("/createTab")
    public void createUrlTab(UrlWebSocketRequestDto.CreateUrlTabRequestDto request, Principal principal) {
        String userEmail = principal.getName();

        UrlWebSocketResponseDto.CreateUrlTabResponseDto response = urlWebSocketService.createUrlTab(userEmail, request);

        //"/topic/workspace.{workspaceID}를 구독한 모든 사용자에게 실시간 탭 생성 알림
        String destination = "/topic/workspace." + request.workspaceId();
        messagingTemplate.convertAndSend(destination, CustomResponse.success(response));
    }

    //URL 탭 삭제 API
    @MessageMapping("/deleteTab")
    public void deleteUrlTab(UrlWebSocketRequestDto.DeleteUrlTabRequestDto request, Principal principal) {
        String userEmail = principal.getName();

        UrlWebSocketResponseDto.DeleteUrlTabResponseDto response = urlWebSocketService.deleteUrlTab(userEmail, request);

        // "/topic/workspace.{workspaceID}"를 구독한 사용자에게 실시간 삭제 알림
        String destination = "/topic/workspace." + request.workspaceId();
        messagingTemplate.convertAndSend(destination, CustomResponse.success(response));
    }

    //URL 탭 이름 변경 API
    @MessageMapping("/updateTabName")
    public void updateUrlTabName(UrlWebSocketRequestDto.UpdateUrlTabNameRequestDto request, Principal principal) {
        String userEmail = principal.getName();

        UrlWebSocketResponseDto.UpdateUrlTabNameResponseDto response = urlWebSocketService.updateUrlTabName(userEmail, request);

        // "/topic/workspace.{workspaceId}" 구독자에게 실시간 이름 변경 알림
        String destination = "/topic/workspace." + request.workspaceId();
        messagingTemplate.convertAndSend(destination, CustomResponse.success(response));
    }

    //URL 아이템 추가 API
    @MessageMapping("/addUrl")
    public void addUrlItem(UrlWebSocketRequestDto.AddUrlItemRequestDto request, Principal principal) {
        String userEmail = principal.getName();

        UrlWebSocketResponseDto.AddUrlItemResponseDto response = urlWebSocketService.addUrlItem(userEmail, request);

        // "/topic/tab.{tabId}"를 구독한 사용자에게 실시간 URL 추가 알림
        String destination = "/topic/tab." + request.tabId();
        messagingTemplate.convertAndSend(destination, CustomResponse.success(response));
    }

    // URL 아이템 삭제 API
    @MessageMapping("/deleteUrl")
    public void deleteUrlItem(UrlWebSocketRequestDto.DeleteUrlItemRequestDto request, Principal principal) {
        String userEmail = principal.getName();

        UrlWebSocketResponseDto.DeleteUrlItemResponseDto response = urlWebSocketService.deleteUrlItem(userEmail, request);

        // "/topic/tab.{tabId}"를 구독한 사용자에게 실시간 삭제 알림
        String destination = "/topic/tab." + request.tabId();
        messagingTemplate.convertAndSend(destination, CustomResponse.success(response));
    }
}
