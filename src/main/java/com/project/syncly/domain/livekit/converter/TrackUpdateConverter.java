package com.project.syncly.domain.livekit.converter;

import com.project.syncly.domain.livekit.dto.TrackUpdateDto;
import com.project.syncly.domain.livekit.enums.LiveKitTrackType;
import com.project.syncly.domain.livekit.enums.LiveKitWebhookType;
import livekit.LivekitModels.ParticipantInfo;
import livekit.LivekitWebhook.WebhookEvent;

public class TrackUpdateConverter {

    public static TrackUpdateDto convert(WebhookEvent event) {
        ParticipantInfo participant = event.getParticipant();
        String identity = participant.getIdentity();
        String room = event.getRoom().getName();

        boolean mic = false;
        boolean screen = false;
        boolean sound = false;

        LiveKitWebhookType eventType = LiveKitWebhookType.from(event.getEvent());
        LiveKitTrackType trackType = LiveKitTrackType.from(event.getTrack().getType().name());

        switch (eventType) {
            case TRACK_UNPUBLISHED -> {
                if (trackType == LiveKitTrackType.MIC) mic = false;
                if (trackType == LiveKitTrackType.SCREEN_SHARE) screen = false;
                if (trackType == LiveKitTrackType.SOUND) sound = true;
            }
            case TRACK_PUBLISHED -> {
                if (trackType == LiveKitTrackType.MIC) mic = true;
                if (trackType == LiveKitTrackType.SCREEN_SHARE) screen = true;
                if (trackType == LiveKitTrackType.SOUND) sound = true;
            }
        }

        return TrackUpdateDto.builder()
                .roomId(room)
                .participantId(identity)
                .micOn(mic)
                .screenSharing(screen)
                .soundOn(sound)
                .build();
    }
}
