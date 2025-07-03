package com.project.syncly.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;
import java.util.Set;


@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    // Value 타입 저장 (단일값)
    public void save(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    // Set 타입 저장
    public boolean addToSet(String key, String value) {
        Long result = redisTemplate.opsForSet().add(key, value);
        return result != null && result > 0;
    }

    // Set 타입 저장 + TTL 7일 (초대코드 저장용)
    public boolean addToSetWithTTL(String key, String value) {
        Long result = redisTemplate.opsForSet().add(key, value);

        if (result != null && result > 0) {
            // 값이 새로 추가되었을 경우에만 TTL 설정 (SET 전체에 TTL)
            redisTemplate.expire(key, 7, TimeUnit.DAYS);
            return true;
        }
        return false;
    }


    public boolean isMemberOfSet(String key, String value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    public Set<Object> getAllFromSet(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    public long removeFromSet(String key, String value) {
        Long result = redisTemplate.opsForSet().remove(key, value);
        return result != null ? result : 0;
    }

    public long getSetSize(String key) {
        Long result = redisTemplate.opsForSet().size(key);
        return result != null ? result : 0;
    }
}

