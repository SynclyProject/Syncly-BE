package com.project.syncly.domain.livekit.service.websocket;

import com.project.syncly.domain.livekit.dto.TrackUpdateDTO;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealtimeNotificationService {

    private final SimpMessagingTemplate messagingTemplate; // Spring WebSocket용

    public void sendTrackUpdate(TrackUpdateDTO dto) {
        messagingTemplate.convertAndSend("/topic/room/" + dto.roomId(), dto);
    }
}
