package com.project.syncly.domain.livekit.dto;

import lombok.Builder;

@Builder
public record TrackUpdateDto(
        String roomId,
        String participantId,
        boolean micOn,
        boolean screenSharing,
        boolean soundOn
) {
}
