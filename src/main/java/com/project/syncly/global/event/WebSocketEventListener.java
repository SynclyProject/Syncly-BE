package com.project.syncly.global.event;

import com.project.syncly.domain.note.service.NoteRedisService;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import java.security.Principal;

/**
 * WebSocket 연결/해제 이벤트를 처리하는 리스너
 *
 * <p>주요 기능:
 * <ul>
 *   <li>SessionConnectedEvent: WebSocket 연결 시 사용자 정보를 Redis에 저장</li>
 *   <li>SessionDisconnectEvent: WebSocket 해제 시 사용자 정보 및 노트 참여 정보 정리</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final RedisTemplate<String, String> redisTemplate;
    private final ApplicationContext applicationContext;
    private final NoteRedisService noteRedisService;



    /**
     * WebSocket 연결 이벤트 처리
     *
     * <p>사용자가 WebSocket에 연결하면 Redis에 세션 정보를 저장합니다.
     *
     * @param event SessionConnectedEvent
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal user = accessor.getUser();

        if (user != null) {
            String userId = user.getName();
            redisTemplate.opsForSet().add(RedisKeyPrefix.WS_ONLINE_USERS.get(), userId);
            redisTemplate.opsForHash().put(RedisKeyPrefix.WS_SESSIONS.get(), sessionId, userId);

            log.info("WebSocket 연결: sessionId={}, userId={}", sessionId, userId);
        }
    }

    /**
     * WebSocket 연결 해제 이벤트 처리
     *
     * <p>사용자가 WebSocket 연결을 끊으면:
     * <ol>
     *   <li>일반 WebSocket 세션 정보 삭제</li>
     *   <li>노트 WebSocket 세션인 경우 노트 참여 정보 정리 (Redis에서 사용자 제거, 커서 삭제)</li>
     * </ol>
     *
     * @param event SessionDisconnectEvent
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        if (!((AbstractApplicationContext) applicationContext).isActive()) {
            log.warn("ApplicationContext 종료로 인한 redis 삭제 생략: sessionId={}", event.getSessionId());
            return;
        }

        String sessionId = event.getSessionId();

        // 1. 일반 WebSocket 세션 정리
        String userId = (String) redisTemplate.opsForHash().get(RedisKeyPrefix.WS_SESSIONS.get(), sessionId);
        if (userId != null) {
            redisTemplate.opsForSet().remove(RedisKeyPrefix.WS_ONLINE_USERS.get(), userId);
            redisTemplate.opsForHash().delete(RedisKeyPrefix.WS_SESSIONS.get(), sessionId);
            log.info("WebSocket 연결 해제: sessionId={}, userId={}", sessionId, userId);
        }

        // 2. 노트 WebSocket 세션 정리 (있는 경우)
        String noteSessionData = (String) redisTemplate.opsForHash()
                .get(RedisKeyPrefix.WS_NOTE_SESSIONS.get(), sessionId);

        if (noteSessionData != null) {
            try {
                // noteSessionData 형식: "noteId:workspaceMemberId"
                String[] parts = noteSessionData.split(":");
                if (parts.length == 2) {
                    Long noteId = Long.parseLong(parts[0]);
                    Long workspaceMemberId = Long.parseLong(parts[1]);

                    // Redis에서 노트 참여 정보 제거
                    noteRedisService.removeUser(noteId, workspaceMemberId);
                    noteRedisService.removeCursor(noteId, workspaceMemberId);

                    // 노트 세션 매핑 삭제
                    redisTemplate.opsForHash().delete(RedisKeyPrefix.WS_NOTE_SESSIONS.get(), sessionId);

                    log.info("노트 WebSocket 세션 정리: sessionId={}, noteId={}, workspaceMemberId={}",
                            sessionId, noteId, workspaceMemberId);
                }
            } catch (Exception e) {
                log.error("노트 세션 정리 중 오류 발생: sessionId={}, noteSessionData={}, error={}",
                        sessionId, noteSessionData, e.getMessage());
            }
        }
    }
}