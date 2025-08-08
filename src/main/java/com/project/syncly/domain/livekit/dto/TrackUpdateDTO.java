package com.project.syncly.domain.livekit.dto;

import com.project.syncly.domain.livekit.enums.TrackSource;
import lombok.Builder;


@Builder
public record TrackUpdateDTO(
        String roomId,
        String participantId,
        boolean isPublish,
        TrackSource trackSource
) {
}
