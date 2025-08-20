package com.project.syncly.domain.livekit.service.redis;

import com.project.syncly.global.redis.core.RedisStorage;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RoomExpirationProducer {

    private final RedisStorage redisStorage;
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
    private static final String ZSET_KEY = RedisKeyPrefix.CALL_ROOM_EVENTS + "";

    public void reserveRoomDeletion(String roomId) {
        long executeAt = System.currentTimeMillis() + DEFAULT_TTL.toMillis();
        redisStorage.addToZSet(ZSET_KEY, roomId, (double) executeAt);
    }

    public void deleteRoomExpiration(String roomId) {
        redisStorage.removeFromZSet(ZSET_KEY, roomId);
    }

}