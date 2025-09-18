package com.project.syncly.domain.livekit.converter;

import com.project.syncly.domain.livekit.dto.ParticipantInfoDTO;
import com.project.syncly.domain.livekit.dto.ParticipantInfoListDTO;

import java.util.List;

public class LiveKitConverter {
    public static String getRoomId(Long workspaceId) {
        return "workspace-" + workspaceId;
    }
    public static ParticipantInfoDTO toParticipantInfoDTO(String participantId,String participantName ,String profileImageObjectKey, boolean audioSharing, boolean screenSharing, boolean isMe) {
        return ParticipantInfoDTO.builder()
                .participantId(participantId)
                .participantName(participantName)
                .profileImageObjectKey(profileImageObjectKey)
                .audioSharing(audioSharing)
                .screenSharing(screenSharing)
                .isMe(isMe)
                .build();
    }
    public static ParticipantInfoListDTO toParticipantInfoListDTO(String roomId, List<ParticipantInfoDTO> list) {
        return ParticipantInfoListDTO.builder()
                .roomId(roomId)
                .participants(list)
                .build();
    }
}
