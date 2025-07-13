package com.project.syncly.domain.livekit.service.websocket;

import com.project.syncly.domain.livekit.dto.NotificationDto;
import com.project.syncly.domain.livekit.dto.TrackUpdateDto;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealtimeNotificationService {

    private final SimpMessagingTemplate messagingTemplate; // Spring WebSocket용

    // 페이지 접속 시 구독, 통화 종료 시 따로 요청받아 구독
    public void sendJoined(String room, String identity) {
        messagingTemplate.convertAndSend("/topic/room/" + room, new NotificationDto("join", identity));
    }

    // 통화 연결 시 구독 해제
    public void sendLeft(String room, String identity) {
        messagingTemplate.convertAndSend("/topic/room/" + room, new NotificationDto("leave", identity));
    }

    public void sendTrackUpdate(TrackUpdateDto dto) {
        messagingTemplate.convertAndSend("/topic/room/" + dto.roomId(), dto);
    }
}
