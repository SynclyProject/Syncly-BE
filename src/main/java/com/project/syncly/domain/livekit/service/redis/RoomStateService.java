package com.project.syncly.domain.livekit.service.redis;

import com.project.syncly.global.redis.core.RedisStorage;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoomStateService {

    private final RedisStorage redisStorage;

    public boolean addParticipant(String roomId, String participantId) {
        String key = RedisKeyPrefix.CALL_ROOM.get(roomId);
        Long count = redisStorage.addToSet(key, participantId);
        return count == 1; // 첫 입장자면 true
    }

    public Set<Object> getParticipantsByRoom(String roomId){
        return redisStorage.getSetValues(roomId);
    }

    public boolean removeParticipant(String roomId, String participantId) {
        String key = RedisKeyPrefix.CALL_ROOM.get(roomId);
        Long count = redisStorage.removeFromSet(key, participantId);
        return count == 0; // 첫 입장자면 true
    }

    public void removeRoom(String roomId) {
        String key = RedisKeyPrefix.CALL_ROOM.get(roomId);
        redisStorage.delete(key);

    }
}