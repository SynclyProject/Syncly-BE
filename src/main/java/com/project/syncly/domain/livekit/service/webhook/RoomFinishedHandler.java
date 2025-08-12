package com.project.syncly.domain.livekit.service.webhook;

import com.project.syncly.domain.livekit.enums.LiveKitWebhookType;
import com.project.syncly.domain.livekit.service.redis.RoomExpirationProducer;
import com.project.syncly.domain.livekit.service.redis.RoomStateService;
import com.project.syncly.domain.livekit.service.websocket.RealtimeNotificationService;
import livekit.LivekitWebhook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomFinishedHandler implements WebhookEventHandler {
    private final RoomStateService roomStateService;
    private final RealtimeNotificationService notificationService;

    @Override
    public boolean supports(LivekitWebhook.WebhookEvent event) {
        return LiveKitWebhookType.from(event.getEvent()) == LiveKitWebhookType.ROOM_FINISHED;
    }

    @Override
    public void handle(LivekitWebhook.WebhookEvent event) {
        String roomId = event.getRoom().getName();

        // 룸 상태 갱신: 종료 처리
        roomStateService.removeRoomAllData(roomId);
        System.out.println("방 종료됨: " + roomId);
        //알림 전송
        notificationService.roomFinished(roomId);
    }
}
