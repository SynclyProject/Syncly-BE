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
        redisStorage.deleteFieldFromHash(key, participantId);
    }
    public void removeAllParticipants(String roomId) {
        String key = RedisKeyPrefix.CALL_ROOM.get(roomId);
        Set<Object> participantIds = redisStorage.getSetValues(key);
        participantIds.forEach(participantId -> {
            redisStorage.delete(
                    RedisKeyPrefix.CALL_PARTICIPANT
                            .format(roomId, participantId.toString()));});

    }

    public void updateTrackState(TrackUpdateDto dto) {
        String key = RedisKeyPrefix.CALL_PARTICIPANT.format(dto.roomId(), dto.participantId());

        // 기존 데이터 가져오기
        Map<Object, Object> existing = redisStorage.getHash(key);
        Object raw = existing.get(dto.participantId());

        if (raw instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> updated = new HashMap<>((Map<String, Object>) raw);
            updated.put("micOn", dto.micOn());
            updated.put("screenSharing", dto.screenSharing());
            redisStorage.updateHashField(key, dto.participantId(), updated);
        } else {
            // 초기화된 적 없는 경우 새로 생성
            Map<String, Object> newState = new HashMap<>();
            newState.put("micOn", dto.micOn());
            newState.put("screenSharing", dto.screenSharing());
            redisStorage.updateHashField(key, dto.participantId(), newState);
        }
    }
}