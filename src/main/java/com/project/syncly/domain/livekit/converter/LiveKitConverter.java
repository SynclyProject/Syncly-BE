package com.project.syncly.domain.livekit.converter;

import com.project.syncly.domain.livekit.dto.ParticipantInfoDTO;
import com.project.syncly.domain.livekit.dto.ParticipantInfoListDTO;

import java.util.List;

public class LiveKitConverter {
    public static String getRoomId(Long workspaceId) {
        return "workspace-" + workspaceId;
    }
    public static ParticipantInfoDTO toParticipantInfoDTO(String participantId,String profileImageObjectKey, boolean audioSharing, boolean screenSharing) {
        return ParticipantInfoDTO.builder()
                .participantId(participantId)
                .profileImageObjectKey(profileImageObjectKey)
                .audioSharing(audioSharing)
                .screenSharing(screenSharing)
                .build();
    }
    public static ParticipantInfoListDTO toParticipantInfoListDTO(String roomId, List<ParticipantInfoDTO> list) {
        return ParticipantInfoListDTO.builder()
                .roomId(roomId)
                .participants(list)
                .build();
    }
}
