package com.project.syncly.global.redis.core;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.syncly.global.redis.error.RedisErrorCode;
import com.project.syncly.global.redis.error.RedisException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "redis.mode", havingValue = "on")//env에서 바로 읽어옴
public class RedisStorageImpl implements RedisStorage {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    @PostConstruct
    public void check() {
        System.out.println("Redis 사용중");
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public String get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value instanceof String ? (String) value : null;
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public <T> void set(String key, T value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        // 강제 변환
        try {
            return redisObjectMapper.convertValue(value, clazz); // 주입받은 거 사용
        } catch (Exception e) {
            throw new RedisException(RedisErrorCode.CONVERT_FAIL_REDIS_TO_OBJECT);
        }
    }
}
