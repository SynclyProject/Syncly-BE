package com.project.syncly.domain.livekit.service.webhook;

import com.project.syncly.domain.livekit.enums.LiveKitWebhookType;
import com.project.syncly.domain.livekit.service.redis.ParticipantStateService;
import com.project.syncly.domain.livekit.service.redis.RoomExpirationProducer;
import com.project.syncly.domain.livekit.service.redis.RoomStateService;
import com.project.syncly.domain.livekit.service.websocket.RealtimeNotificationService;
import livekit.LivekitModels.Room;
import livekit.LivekitModels.ParticipantInfo;
import livekit.LivekitWebhook.WebhookEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParticipantJoinedHandler implements WebhookEventHandler {

    private final ParticipantStateService participantStateService;
    private final RoomStateService roomStateService;
    private final RoomExpirationProducer roomExpirationProducer;
    private final RealtimeNotificationService notificationService;

    @Override
    public boolean supports(WebhookEvent event) {
        return LiveKitWebhookType.from(event.getEvent()) == LiveKitWebhookType.PARTICIPANT_JOINED;
    }

    @Override
    public void handle(WebhookEvent event) {
        String roomId = event.getRoom().getName();
        String participantId = event.getParticipant().getIdentity();
        long joinedAt = event.getParticipant().getJoinedAt();

        // 참가자 정보 저장 (track 상태 false로 초기화)
        participantStateService.registerParticipant(roomId, participantId, joinedAt);

        // Set에 추가하고 첫 참가자라면 Stream에 삭제 예약 추가
        boolean isFirst = roomStateService.addParticipant(roomId, participantId);
        if (isFirst) {
            roomExpirationProducer.reserveRoomDeletion(roomId);
        }

    }
}