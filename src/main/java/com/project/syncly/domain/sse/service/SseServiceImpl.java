package com.project.syncly.domain.sse.service;

import com.project.syncly.domain.sse.converter.SseConverter;
import com.project.syncly.domain.sse.dto.SseResponseDto;
import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspace.entity.WorkspaceInvitation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseServiceImpl implements SseService{
    //단일 브라우저 연결(Map<Long, SseEmitter>) 구조
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(Long memberId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); //타임아웃 설정, 브라우저에서 자동 재연결
        emitters.put(memberId, emitter); //해당 유저의 emitter 객체 저장

        emitter.onCompletion(() -> emitters.remove(memberId));
        emitter.onTimeout(() -> emitters.remove(memberId));
        emitter.onError((e) -> emitters.remove(memberId));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE 연결 완료"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    @Override
    public void sendInvitationAlertToUser(Long memberId, WorkspaceInvitation invitation, Workspace workspace) {
        SseEmitter emitter = emitters.get(memberId);
        if (emitter != null) {
            try {
                // DTO 생성
                SseResponseDto.InvitedNotificationResponseDto dto =
                        SseConverter.toInvitedNotification(invitation, workspace);

                // SSE 알림 전송
                emitter.send(SseEmitter.event()
                        .name("invitation") // 이벤트 이름 명확히 구분
                        .data(dto, MediaType.APPLICATION_JSON));

                log.info("[SSE] Sent invitation notification to user {}", memberId);

            } catch (IOException e) {
                emitter.completeWithError(e);
                // 연결이 동일한 emitter일 때만 삭제
                if (emitters.get(memberId) == emitter) {
                    emitters.remove(memberId);
                    log.info("[SSE] Removed emitter for user {} after failure", memberId);
                }
            }
        }
    }
}
