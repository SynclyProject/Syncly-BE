package com.project.syncly.global.event;

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

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final RedisTemplate<String, String> redisTemplate;
    private final ApplicationContext applicationContext;



    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal user = accessor.getUser();

        if (user != null) {
            String userId = user.getName();
            redisTemplate.opsForSet().add(RedisKeyPrefix.WS_ONLINE_USERS.get(), userId);
            redisTemplate.opsForHash().put(RedisKeyPrefix.WS_SESSIONS.get(), sessionId, userId);

            log.info("User connected: {}", userId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        if (!((AbstractApplicationContext) applicationContext).isActive()) {
            log.warn("ApplicationContext 종료로 인한 redis 삭제 생략 {}", event.getSessionId());
            return;
        }

        String sessionId = event.getSessionId();

        String userId = (String) redisTemplate.opsForHash().get(RedisKeyPrefix.WS_SESSIONS.get(), sessionId);
        if (userId != null) {
            redisTemplate.opsForSet().remove(RedisKeyPrefix.WS_ONLINE_USERS.get(), userId);
            redisTemplate.opsForHash().delete(RedisKeyPrefix.WS_SESSIONS.get(), sessionId);
            log.info("User disconnected: {}", userId);
        }
    }
}