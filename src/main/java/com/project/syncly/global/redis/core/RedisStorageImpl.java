package com.project.syncly.global.redis.core;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.syncly.global.redis.error.RedisErrorCode;
import com.project.syncly.global.redis.error.RedisException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.HashOperations;


import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "redis.mode", havingValue = "on")//env에서 바로 읽어옴
public class RedisStorageImpl implements RedisStorage {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
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

    @Override
    public Long addToSet(String key, String value) {
        stringRedisTemplate.opsForSet().add(key, value);
        return stringRedisTemplate.opsForSet().size(key);
    }

    @Override
    public Long removeFromSet(String key, String value) {
        stringRedisTemplate.opsForSet().remove(key, value);
        return stringRedisTemplate.opsForSet().size(key);
    }
    @Override
    public Set<String> getSetValues(String key) {
        return stringRedisTemplate.opsForSet().members(key);
    }

    @Override
    public Boolean addToZSet(String key, String value, double score) {
        return stringRedisTemplate.opsForZSet().add(key, value, score);
    }

    @Override
    public Long removeFromZSet(String key, String value) {
        return stringRedisTemplate.opsForZSet().remove(key, value);
    }

    @Override
    public Set<String> getZSetByScoreRange(String key, double minScore, double maxScore) {
        return stringRedisTemplate.opsForZSet().rangeByScore(key, minScore, maxScore);
    }

    @Override
    public void setHash(String key, Map<String, Object> values, Duration ttl) {
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();
        ops.putAll(key, values);
        redisTemplate.expire(key, ttl);
    }

    @Override
    public void updateHashField(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    @Override
    public Map<String, Object> getHash(String key) {
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();
        return ops.entries(key);
    }
}
