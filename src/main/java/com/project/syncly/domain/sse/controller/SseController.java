package com.project.syncly.domain.sse.controller;

import com.project.syncly.domain.sse.service.SseServiceImpl;
import com.project.syncly.global.jwt.JwtProvider;
import com.project.syncly.global.jwt.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
@Tag(name = "SSE 알림 연결 API")
public class SseController {

    private final SseServiceImpl sseService;
    private final JwtProvider jwtProvider;

    //알림 수신 구독 API
    @Operation(summary = "알림 수신 구독 API (swagger로 테스트 불가: 무한로딩)")
    @GetMapping(value = "/notifications", produces = "text/event-stream") //SSE임을 명시
    public ResponseEntity<SseEmitter> subscribe(@RequestParam("token") String token) {
        Long memberId = jwtProvider.getMemberId(token); // 토큰에서 ID 추출
        SseEmitter emitter = sseService.subscribe(memberId);
        return ResponseEntity.ok(emitter);
    }


}

