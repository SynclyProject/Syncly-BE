package com.project.syncly.domain.livekit.enums;

public enum LiveKitTrackType {
    MIC,
    SCREEN_SHARE,
    SOUND,
    UNKNOWN;

    public static LiveKitTrackType from(String type) {
        try {
            return LiveKitTrackType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;//webhook 받을 트랙 선택 불가하므로 unknown 처리 후 무시.
        }
    }
}
