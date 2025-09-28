package com.project.syncly.domain.livekit.service.websocket;

import com.project.syncly.domain.livekit.dto.RoomRealtimeEventDTO;
import com.project.syncly.domain.livekit.dto.TrackUpdateDTO;
import com.project.syncly.domain.livekit.enums.LiveKitWebhookType;
import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.service.MemberQueryService;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealtimeNotificationService {

    private final SimpMessagingTemplate messagingTemplate; // Spring WebSocket용
    private final MemberQueryService memberQueryService;

    public void roomStarted(String roomId) {
        RoomRealtimeEventDTO.RoomEvent detail = RoomRealtimeEventDTO.RoomEvent.builder()
                .isStart(true)
                .build();
        RoomRealtimeEventDTO dto = RoomRealtimeEventDTO.builder()
                .type(LiveKitWebhookType.ROOM_STARTED)
                .roomId(roomId)
                .room(detail)
                .build();
        messagingTemplate.convertAndSend("/topic/room/" + roomId, dto);
    }
    public void roomFinished(String roomId) {
        RoomRealtimeEventDTO.RoomEvent detail = RoomRealtimeEventDTO.RoomEvent.builder()
                .isStart(false)
                .build();
        RoomRealtimeEventDTO dto = RoomRealtimeEventDTO.builder()
                .type(LiveKitWebhookType.ROOM_FINISHED)
                .roomId(roomId)
                .room(detail)
                .build();
        messagingTemplate.convertAndSend("/topic/room/" + roomId, dto);
    }

    public void participantJoined(String roomId, String participantId) {
        Member member = memberQueryService.getMemberByIdWithRedis(Long.parseLong(participantId));
        RoomRealtimeEventDTO.ParticipantEvent detail = RoomRealtimeEventDTO.ParticipantEvent.builder()
                .isJoined(true)
                .participantId(participantId)
                .memberName(member.getName())
                .memberProfileObjectKey(member.getProfileImage())
                .build();
        RoomRealtimeEventDTO dto = RoomRealtimeEventDTO.builder()
                                        .type(LiveKitWebhookType.PARTICIPANT_JOINED)
                                        .roomId(roomId)
                                        .participant(detail)
                                        .build();

        messagingTemplate.convertAndSend("/topic/room/" + roomId, dto);
    }
    public void participantLeft(String roomId, String participantId) {

        RoomRealtimeEventDTO.ParticipantEvent detail = RoomRealtimeEventDTO.ParticipantEvent.builder()
                .isJoined(false)
                .participantId(participantId)
                .build();
        RoomRealtimeEventDTO dto = RoomRealtimeEventDTO.builder()
                .type(LiveKitWebhookType.PARTICIPANT_LEFT)
                .roomId(roomId)
                .participant(detail)
                .build();

        messagingTemplate.convertAndSend("/topic/room/" + roomId, dto);
    }
    public void sendTrackUpdate(TrackUpdateDTO dto) {
        messagingTemplate.convertAndSend("/topic/room/" + dto.roomId(), dto);
    }
}
