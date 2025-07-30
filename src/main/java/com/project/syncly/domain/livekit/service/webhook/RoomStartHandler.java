package com.project.syncly.domain.livekit.service.webhook;

import com.project.syncly.domain.livekit.enums.LiveKitWebhookType;
import com.project.syncly.domain.livekit.service.redis.RoomExpirationProducer;
import com.project.syncly.domain.livekit.service.redis.RoomStateService;
import livekit.LivekitWebhook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomStartHandler implements WebhookEventHandler {
    private final RoomStateService roomStateService;
    private final RoomExpirationProducer roomExpirationProducer;
    @Override
    public boolean supports(LivekitWebhook.WebhookEvent event) {
        return LiveKitWebhookType.from(event.getEvent()) == LiveKitWebhookType.ROOM_STARTED;
    }

    @Override
    public void handle(LivekitWebhook.WebhookEvent event) {
        String roomId = event.getRoom().getName();
        //삭제예약 시작
        roomExpirationProducer.reserveRoomDeletion(roomId);
        System.out.println("방 시작됨: " + roomId);
    }
}
