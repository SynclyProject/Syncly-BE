package com.project.syncly.domain.livekit.service.redis;

import com.project.syncly.domain.livekit.converter.LiveKitConverter;
import com.project.syncly.domain.livekit.dto.ParticipantInfoDTO;
import com.project.syncly.domain.livekit.dto.TrackUpdateDTO;
import com.project.syncly.domain.livekit.enums.TrackSource;
import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.service.MemberQueryService;
import com.project.syncly.global.redis.core.RedisStorage;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ParticipantStateService {

    private final RedisStorage redisStorage;
    private final MemberQueryService memberQueryService;

    private static final Duration TTL = Duration.ofMinutes(60); // 1시간

    public ParticipantInfoDTO getParticipantInfo(String roomId, String participantId) {
        String key = RedisKeyPrefix.CALL_PARTICIPANT.format(roomId, participantId);
        Map<String, Object> data = redisStorage.getHash(key);

        if (data == null || data.isEmpty()) {
            return null;
        }
        Member member = memberQueryService.getMemberByIdWithRedis(Long.parseLong(participantId));
        return LiveKitConverter.toParticipantInfoDTO(
                participantId,
                member.getProfileImage(),
                Boolean.parseBoolean(String.valueOf(data.getOrDefault("audioSharing", false))),
                Boolean.parseBoolean(String.valueOf(data.getOrDefault("screenSharing", false)))
        );
    }

    public void removeParticipant(String roomId, String participantId) {
        String key = RedisKeyPrefix.CALL_PARTICIPANT.format(roomId, participantId);
        redisStorage.delete(key);
    }
    public void removeAllParticipants(String roomId) {
        String key = RedisKeyPrefix.CALL_ROOM.get(roomId);
        Set<String> participantIds = redisStorage.getSetValues(key);
        participantIds.forEach(participantId -> {
            redisStorage.delete(
                    RedisKeyPrefix.CALL_PARTICIPANT
                            .format(roomId, participantId));});

    }
    public void initParticipantFields(String roomId, String participantId, long joinedAt) {
        String key = RedisKeyPrefix.CALL_PARTICIPANT.format(roomId, participantId);

        Map<String, Object> values = new HashMap<>();
        values.put("joinedAt", joinedAt);
        values.put("audioSharing", false);
        values.put("screenSharing", false);
        redisStorage.setHash(key, values, TTL);
    }

    public void updateTrackState(TrackUpdateDTO dto) {
        String key = RedisKeyPrefix.CALL_PARTICIPANT.format(dto.roomId(), dto.participantId());
        if (TrackSource.MICROPHONE.equals(dto.trackSource())) {
            redisStorage.updateHashField(key, "audioSharing", dto.isPublish());
        } else if (TrackSource.SCREEN_SHARE.equals(dto.trackSource())) {
            redisStorage.updateHashField(key, "screenSharing", dto.isPublish());
        }
    }
}