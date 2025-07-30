package com.project.syncly.domain.livekit.service.redis;

import com.project.syncly.domain.livekit.dto.TrackUpdateDto;
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

    private static final Duration TTL = Duration.ofMinutes(60); // 1시간

    public void registerParticipant(String roomId, String participantId, long joinedAt) {

        String key = RedisKeyPrefix.CALL_PARTICIPANT.format(roomId, participantId);

        Map<String, Object> participantInfo = new HashMap<>();
        participantInfo.put("joinedAt", joinedAt);
        participantInfo.put("micOn", false);
        participantInfo.put("soundOn", false);
        participantInfo.put("screenSharing", false);

        redisStorage.updateHashField(key, participantId, participantInfo);
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