package com.project.syncly.domain.livekit.controller;

import com.project.syncly.domain.livekit.exception.LiveKitErrorCode;
import com.project.syncly.domain.livekit.exception.LiveKitException;
import com.project.syncly.domain.livekit.service.LiveKitTokenService;
import com.project.syncly.domain.livekit.service.WebhookEventRouter;
import com.project.syncly.global.anotations.MemberIdInfo;
import com.project.syncly.global.apiPayload.CustomResponse;
import jakarta.servlet.http.HttpServletRequest;
import livekit.LivekitWebhook.WebhookEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.livekit.server.WebhookReceiver;


import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/livekit")
@RequiredArgsConstructor
public class LiveKitController {

    private final LiveKitTokenService liveKitTokenService;
    private final WebhookEventRouter webhookEventRouter;
    private final WebhookReceiver webhookReceiver;

    @GetMapping("/token")
    public ResponseEntity<CustomResponse<String>> getLiveKitToken(
            @RequestParam("workspaceId") Long workspaceId,
            @MemberIdInfo Long memberId) {
        String token = liveKitTokenService.issueToken(memberId, workspaceId);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, token));
    }

    //초기 참가자 정보 조회
    @GetMapping("/init-info")
    public ResponseEntity<CustomResponse<ParticipantInfoListDTO>> getParticipantInfo(
            @RequestParam("workspaceId") Long workspaceId,
            @MemberIdInfo Long memberId) {
        ParticipantInfoListDTO infos = roomStateService.getParticipantInfoList(workspaceId, memberId);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, infos));
    }
    //방 삭제 테스트
    @DeleteMapping("/twirp/livekit.RoomService/DeleteRoom")
    public ResponseEntity<CustomResponse<Void>> deleteLiveKitServerRoom(
            @RequestParam("roomId") String roomId,
            @MemberIdInfo Long memberId){
        roomStateService.deleteRoomTester(roomId);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK));
    }

    @PostMapping(value = "/webhook", consumes = {"application/webhook+json", "application/json"})
    public ResponseEntity<Void> handleWebhook(HttpServletRequest request) {
        try {
            String body = new BufferedReader(request.getReader())
                    .lines()
                    .collect(Collectors.joining(System.lineSeparator()));

            String auth = request.getHeader("Authorization");
            System.out.println("LiveKit Webhook 호출됨! " + body);

            WebhookEvent event = webhookJwtVerifier.getReceiver().receive(body, auth);
            webhookEventRouter.handle(event);

            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            throw new LiveKitException(LiveKitErrorCode.INVALID_WEBHOOK_SIGNATURE);
        } catch (IOException e) {
            throw new LiveKitException(LiveKitErrorCode.INVALID_WEBHOOK_BODY);
        }
    }
}