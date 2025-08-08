package com.project.syncly.domain.livekit.converter;

import com.project.syncly.domain.livekit.dto.ParticipantInfoDTO;
import com.project.syncly.domain.livekit.dto.ParticipantInfoListDTO;

import java.util.List;

public class LiveKitConverter {
    public static String getRoomId(Long workspaceId) {
        return "workspace-" + workspaceId;
    }
    public static ParticipantInfoDTO toParticipantInfoDTO(String participantId, boolean audioSharing, boolean screenSharing) {
        return ParticipantInfoDTO.builder()
                .participantId(participantId)
                .audioSharing(audioSharing)
                .screenSharing(screenSharing)
                .build();
    }
    public static ParticipantInfoListDTO toParticipantInfoListDTO(List<ParticipantInfoDTO> list) {
        return ParticipantInfoListDTO.builder()
                .participants(list)
                .build();
    }
}
