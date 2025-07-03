package com.project.syncly.global.handler;

import com.project.syncly.global.jwt.JwtProvider;
import com.project.syncly.global.jwt.exception.JwtErrorCode;
import com.project.syncly.global.jwt.exception.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class StompHandler implements ChannelInterceptor {
    private final JwtProvider jwtProvider;

    //WebSocket 서버에서 ChannelInterceptor를 사용해 STOMP CONNECT 요청을 가로채고 헤더를 확인
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String rawToken = accessor.getFirstNativeHeader("Authorization");

            if (rawToken == null || !rawToken.startsWith("Bearer ")) {
                throw new JwtException(JwtErrorCode.INVALID_TOKEN);
            }

            // "Bearer " 제거
            String token = rawToken.substring(7);

            if (!jwtProvider.isValidToken(token)) {
                throw new JwtException(JwtErrorCode.INVALID_TOKEN);
            }

            Authentication authentication = jwtProvider.getAuthentication(token);
            accessor.setUser(authentication);
        }

        return message;
    }
}