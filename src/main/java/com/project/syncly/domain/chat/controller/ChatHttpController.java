package com.project.syncly.domain.chat.controller;

import com.project.syncly.domain.chat.dto.ChatHttpResponseDto;
import com.project.syncly.domain.chat.service.ChatHttpServiceImpl;
import com.project.syncly.global.apiPayload.CustomResponse;
import com.project.syncly.global.jwt.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workspaces")
@Tag(name = "Chat 관련 API (HTTP 조회 API, WebSocket은 분리)")
@Validated
public class ChatHttpController {

    private final ChatHttpServiceImpl chatHttpService;


     //1. 최신 채팅 메시지 조회
     //ex. GET /api/workspaces/{workspaceId}/messages?limit=50
    @GetMapping("/{workspaceId}/messages")
    @Operation(summary = "최신 채팅 메시지 조회 (seq ASC 정렬)")
    public ResponseEntity<CustomResponse<ChatHttpResponseDto.ChatResponseDto>> getLatestMessages(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        ChatHttpResponseDto.ChatResponseDto response =
                chatHttpService.getLatestPage(workspaceId, memberId, limit);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, response));
    }

     //2. 과거 메시지 더보기 (무한 스크롤)
     //ex. GET /api/workspaces/{workspaceId}/messages/before?beforeSeq=123&limit=50
    @GetMapping("/{workspaceId}/messages/before")
    @Operation(summary = "과거 메시지 더보기 (무한 스크롤, seq ASC 정렬)")
    public ResponseEntity<CustomResponse<ChatHttpResponseDto.ChatResponseDto>> getMessagesBefore(
            @PathVariable Long workspaceId,
            @RequestParam Long beforeSeq,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        ChatHttpResponseDto.ChatResponseDto response =
                chatHttpService.getMessagesBefore(workspaceId, memberId, beforeSeq, limit);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, response));
    }

     // 3. 끊김 보정 (재연결 델타)
     //ex. GET /api/workspaces/{workspaceId}/messages/after?afterSeq=10100&limit=200
    @GetMapping("/{workspaceId}/messages/after")
    @Operation(summary = "끊김 보정 (afterSeq 이후 메시지 조회, seq ASC 정렬)")
    public ResponseEntity<CustomResponse<ChatHttpResponseDto.ChatResponseDto>> getMessagesAfter(
            @PathVariable Long workspaceId,
            @RequestParam Long afterSeq,
            @RequestParam(defaultValue = "200") @Min(1) @Max(500) int limit,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        ChatHttpResponseDto.ChatResponseDto response =
                chatHttpService.getMessagesAfter(workspaceId, memberId, afterSeq, limit);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, response));
    }

    //4. 자신의 workspaceMemberId 조회 API (채팅에서 본인 메시지 확인용)
    @GetMapping("/{workspaceId}/me")
    @Operation(summary = "자신의 workspaceMemberId 조회")
    public ResponseEntity<CustomResponse<Long>> getMyWorkspaceMemberId(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        Long workspaceMemberId = chatHttpService.getMyWorkspaceMemberId(workspaceId, memberId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, workspaceMemberId));
    }
}
