package com.project.syncly.domain.livekit.enums;

public enum TrackSource {
    MICROPHONE,
    SCREEN_SHARE,
    UNKNOWN;

    public static TrackSource from(String type) {
        try {
            return TrackSource.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;//webhook 받을 트랙 선택 불가하므로 unknown 처리 후 무시.
        }
    }
}
