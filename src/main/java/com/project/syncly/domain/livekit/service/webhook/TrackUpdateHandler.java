package com.project.syncly.domain.livekit.service.webhook;

import com.project.syncly.domain.livekit.enums.LiveKitWebhookType;
import com.project.syncly.domain.livekit.converter.TrackUpdateConverter;
import com.project.syncly.domain.livekit.dto.TrackUpdateDTO;
import com.project.syncly.domain.livekit.service.redis.ParticipantStateService;
import com.project.syncly.domain.livekit.service.websocket.RealtimeNotificationService;
import livekit.LivekitWebhook.WebhookEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackUpdateHandler implements WebhookEventHandler {

    private final ParticipantStateService participantStateService;
    private final RealtimeNotificationService notificationService;

    @Override
    public boolean supports(WebhookEvent event) {
        return LiveKitWebhookType.TRACK_EVENTS.contains(LiveKitWebhookType.from(event.getEvent()));
    }

    @Override
    public void handle(WebhookEvent event) {
        TrackUpdateDTO dto = TrackUpdateConverter.convert(event);
        participantStateService.updateTrackState(dto);
        //알림 전송
        notificationService.sendTrackUpdate(dto);
    }
}