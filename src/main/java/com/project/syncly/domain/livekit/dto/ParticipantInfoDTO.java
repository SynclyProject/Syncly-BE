package com.project.syncly.domain.livekit.dto;

import livekit.LivekitModels;
import lombok.Builder;

@Builder
public record ParticipantInfoDTO(
        String participantId,
        String profileImageObjectKey,
        boolean audioSharing,
        boolean screenSharing
) {}

