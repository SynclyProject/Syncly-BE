package com.project.syncly.domain.livekit.service.webhook;

import com.project.syncly.domain.livekit.enums.LiveKitWebhookType;
import com.project.syncly.domain.livekit.service.redis.ParticipantStateService;
import com.project.syncly.domain.livekit.service.redis.RoomStateService;
import com.project.syncly.domain.livekit.service.websocket.RealtimeNotificationService;
import livekit.LivekitModels.ParticipantInfo;
import livekit.LivekitModels.Room;
import livekit.LivekitWebhook.WebhookEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParticipantLeftHandler implements WebhookEventHandler {

    private final ParticipantStateService participantStateService;
    private final RoomStateService roomStateService;
    @Override
    public boolean supports(WebhookEvent event) {
        return LiveKitWebhookType.from(event.getEvent()) == LiveKitWebhookType.PARTICIPANT_LEFT;
    }

    @Override
    public void handle(WebhookEvent event) {
        String roomId = event.getRoom().getName();
        String participantId = event.getParticipant().getIdentity();

        participantStateService.removeParticipant(
                roomId,
                participantId
        );
        roomStateService.removeParticipant(
                roomId,
                participantId);
    }
}
