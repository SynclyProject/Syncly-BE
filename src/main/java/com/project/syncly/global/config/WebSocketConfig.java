package com.project.syncly.global.config;

import com.project.syncly.global.handler.StompHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker //STOMP를 활용한 WebSocket 메시징 기능 활성화
@Order(Ordered.HIGHEST_PRECEDENCE + 99) // Spring Security보다 먼저 실행되도록 우선순위 지정
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompHandler stompHandler; // jwt 토큰 인증 핸들러

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    } //클라이언트가 메시지를 보낼 때 /app/으로 시작을 하게 되면
    //서버쪽 컨트롤러 클래스 내부의 @MessageMapping으로 라우팅

    @Override //클라이언트가 웹소켓에 연결할 때 사용할 엔드포인트를 등록
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp") //클라이언트가 웹소켓 서버에 최초로 접속할 때 연결할 주소
                .setAllowedOriginPatterns("*");  // CORS 허용 설정, 실 서비스 시에는 도메인을 제한
                //.withSockJS();  // SockJS를 사용하여 연결을 시도
    }

    @Override //토큰을 가진 유저와 웹소켓을 연결할 것이므로, 토큰을 검증하는 로직이 필요
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler);
    }

    @Override  //웹소켓 메시징 처리에서 메시지 크기, 버퍼, 전송 시간 제한 같은 전송 계층 옵션 설정
    public void configureWebSocketTransport(WebSocketTransportRegistration reg) {
        reg.setMessageSizeLimit(1 * 1024 * 1024)       // 메시지 크기 제한 (바이트)
                .setSendBufferSizeLimit(10 * 1024 * 1024)   // 세션별 전송 버퍼 제한 (바이트)
                .setSendTimeLimit(15_000);                  // 메시지 전송 제한 시간 (ms)
    }

}
