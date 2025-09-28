package com.project.syncly.domain.auth.whitelist;

import com.project.syncly.global.redis.core.RedisStorage;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshWhitelistService {

    private final RedisStorage redisStorage;

    private static final String ROTATE_LUA = """
    local cur = redis.call('GET', KEYS[1])
    if cur ~= ARGV[1] then return 0 end
    redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
    return 1
    """;
/*
1. 저장된 jti와 요청 jti가 다르면 → MISMATCH
2. 같으면 새 jti로 교체 → SUCCESS

jwt토큰은 위조 불가이므로, userId+deviceId의 JTI가 다르다면 이미 Rotate된 탈취된 토큰이라고 본다
*/

    public RotateResult rotateAtomic(long userId, String deviceId, String oldJti, String newJti, long ttlSec, String uaHash) {
        String currentKey = RedisKeyPrefix.REFRESH_CURRENT.format(Long.toString(userId), deviceId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(ROTATE_LUA, Long.class);
        Long r = redisStorage.executeScript(
                script,
                List.of(currentKey),
                oldJti, newJti, String.valueOf(ttlSec)
        );

        if (r == null) return RotateResult.MISMATCH;
        if (r == 1L)   return RotateResult.SUCCESS;
        return RotateResult.MISMATCH; // 0이면 mismatch
    }

    public void cashUaHash(String uaHash, long userId, String deviceId) {
        if (uaHash != null) {
            String uaHashKey = RedisKeyPrefix.CASHED_UA_HASH.format(Long.toString(userId), deviceId);
            redisStorage.setValueAsString(uaHashKey, uaHash, Duration.ofSeconds(10)); // TTL은 비교용이니 10초 정도로만
        }
    }

    public boolean validateUaHash(String uaHash, long userId, String deviceId) {
        if (uaHash == null) {
            return false; // UA 없는 경우 비교 불가
        }
        String uaHashKey = RedisKeyPrefix.CASHED_UA_HASH.format(Long.toString(userId), deviceId);
        String savedUaHash = redisStorage.getValueAsString(uaHashKey);
        return savedUaHash != null && savedUaHash.equals(uaHash);
    }

    //화이트리스트에서 해당 userId+deviceId 키를 삭제(세션 강제 만료)
    public void evict(long userId, String deviceId) {
        String currentKey = RedisKeyPrefix.REFRESH_CURRENT.format(Long.toString(userId), deviceId);
        redisStorage.delete(currentKey);

        String uaHashKey = RedisKeyPrefix.CASHED_UA_HASH.format(Long.toString(userId), deviceId);
        redisStorage.delete(uaHashKey);
    }

    public void setCurrent(long userId, String deviceId, String jti, long ttlSec) {
        redisStorage.setValueAsString(
                RedisKeyPrefix.REFRESH_CURRENT.format(Long.toString(userId), deviceId),
                jti,
                Duration.ofSeconds(ttlSec)
        );
    }
}
