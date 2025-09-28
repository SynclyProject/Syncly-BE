package com.project.syncly.domain.livekit.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.syncly.domain.livekit.enums.LiveKitWebhookType;
import com.project.syncly.domain.livekit.enums.TrackSource;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record RoomRealtimeEventDTO(
        LiveKitWebhookType type,
        String roomId,
        ParticipantEvent participant, // 참가자 이벤트 관련 필드
        RoomEvent room                // 방 생성/삭제 등
) {
    @Builder
    public record ParticipantEvent(
            boolean isJoined,
            String participantId,
            String memberName,
            String memberProfileObjectKey
    ) {}
    @Builder
    public record RoomEvent(
            boolean isStart
    ) {}
}