package com.project.syncly.domain.livekit.job;

import com.project.syncly.domain.livekit.exception.LiveKitErrorCode;
import com.project.syncly.domain.livekit.exception.LiveKitException;
import com.project.syncly.domain.livekit.service.api.LiveKitApiService;
import com.project.syncly.domain.livekit.service.redis.ParticipantStateService;
import com.project.syncly.domain.livekit.service.redis.RoomExpirationProducer;
import com.project.syncly.domain.livekit.service.redis.RoomStateService;
import com.project.syncly.global.redis.core.RedisStorage;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomExpirationWorker {

    private final RedisStorage redisStorage;
    private final RoomStateService roomStateService;
    private final ParticipantStateService participantStateService;
    private final LiveKitApiService liveKitApiService;
    private final RoomExpirationProducer roomExpirationProducer;

    private static final String ZSET_KEY = RedisKeyPrefix.CALL_ROOM_EVENTS + "";

    @Scheduled(fixedDelay = 60000) // 60초마다 실행
    public void consume() {
        long now = System.currentTimeMillis();

        Set<Object> expiredRoomIds = redisStorage.getZSetByScoreRange(ZSET_KEY, 0, now);
        if (expiredRoomIds == null || expiredRoomIds.isEmpty()) return;

        for (Object obj : expiredRoomIds) {
            String roomId = obj.toString();
            log.info("방 삭제 (만료됨): {}", roomId);

            try {
                liveKitApiService.deleteRoom(roomId); // LiveKit에서 방 삭제
                roomStateService.removeRoom(roomId);  // Redis set 제거
                participantStateService.removeAllParticipants(roomId); // Redis hash 제거
            } catch (Exception e) {
                log.error("방 삭제 중 오류 발생: {}", roomId, e);
                //명시적 예외 던지지 않고 스킵처리
            }

            roomExpirationProducer.deleteRoomExpiration(roomId); // ZSet에서 삭제
        }
    }
}