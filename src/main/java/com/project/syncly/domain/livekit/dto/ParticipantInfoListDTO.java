package com.project.syncly.domain.livekit.dto;

import lombok.Builder;

import java.util.List;
@Builder
public record ParticipantInfoListDTO(
        String roomId,
        List<ParticipantInfoDTO> participants
) {}
