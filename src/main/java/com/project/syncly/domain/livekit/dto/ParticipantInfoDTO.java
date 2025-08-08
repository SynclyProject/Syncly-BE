package com.project.syncly.domain.livekit.dto;

import livekit.LivekitModels;
import lombok.Builder;

@Builder
public record ParticipantInfoDTO(
        String participantId,
        boolean audioSharing,
        boolean screenSharing
) {}

