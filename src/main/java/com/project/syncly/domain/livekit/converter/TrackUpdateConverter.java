package com.project.syncly.domain.livekit.converter;

import com.project.syncly.domain.livekit.dto.TrackUpdateDTO;
import com.project.syncly.domain.livekit.enums.LiveKitWebhookType;
import com.project.syncly.domain.livekit.enums.TrackSource;
import livekit.LivekitWebhook.WebhookEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TrackUpdateConverter {

    public static TrackUpdateDTO convert(WebhookEvent event) {
        LiveKitWebhookType type = LiveKitWebhookType.from(event.getEvent());
        String identity = event.getParticipant().getIdentity();
        String room = event.getRoom().getName();
        boolean isPublish = type.equals(LiveKitWebhookType.TRACK_PUBLISHED);
        TrackSource trackSource = TrackSource.from(String.valueOf(event.getTrack().getSource()));
        if (trackSource == TrackSource.UNKNOWN) {
            log.warn("지원하지 않는 트랙 소스 수신: {}", event.getTrack().getSource());
        }
        return TrackUpdateDTO.builder()
                .roomId(room)
                .participantId(identity)
                .isPublish(isPublish)
                .trackSource(trackSource)
                .build();
    }
}
