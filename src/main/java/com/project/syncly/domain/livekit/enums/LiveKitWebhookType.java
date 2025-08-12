package com.project.syncly.domain.livekit.enums;

import java.util.Arrays;
import java.util.EnumSet;

public enum LiveKitWebhookType {
    ROOM_STARTED("room_started"),
    PARTICIPANT_JOINED("participant_joined"),
    PARTICIPANT_LEFT("participant_left"),
    TRACK_PUBLISHED("track_published"),
    TRACK_UNPUBLISHED("track_unpublished"),
    ROOM_FINISHED("room_finished"),
    UNKNOWN("unknown");

    private final String value;

    LiveKitWebhookType(String value) {
        this.value = value;
    }

    public static LiveKitWebhookType from(String value) {
        return Arrays.stream(values())
                .filter(e -> e.value.equalsIgnoreCase(value))
                .findFirst()
                .orElse(UNKNOWN);
    }

    public static final EnumSet<LiveKitWebhookType> TRACK_EVENTS =
            EnumSet.of(TRACK_PUBLISHED, TRACK_UNPUBLISHED);

}
