package com.project.syncly.global.redis.core;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.syncly.global.redis.error.RedisErrorCode;
import com.project.syncly.global.redis.error.RedisException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;


import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Override
    public Long addToSet(String key, String value) {
        redisTemplate.opsForSet().add(key, value);
        return redisTemplate.opsForSet().size(key);
    }

    @Override
    public Long removeFromSet(String key, String value) {
        redisTemplate.opsForSet().remove(key, value);
        return redisTemplate.opsForSet().size(key);
    }
    @Override
    public Set<Object> getSetValues(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    @Override
    public Boolean addToZSet(String key, String value, double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    @Override
    public Long removeFromZSet(String key, String value) {
        return redisTemplate.opsForZSet().remove(key, value);
    }

    @Override
    public Set<Object> getZSetByScoreRange(String key, double minScore, double maxScore) {
        return redisTemplate.opsForZSet().rangeByScore(key, minScore, maxScore);
    }

    @Override
    public Double getZSetScore(String key, String value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    @Override
    public Long getZSetSize(String key) {
        return redisTemplate.opsForZSet().zCard(key);
    }


    @Override
    public void setHash(String key, Map<String, Object> values, Duration ttl) {
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        ops.putAll(key, values);
        redisTemplate.expire(key, ttl);
    }

    @Override
    public void updateHashField(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    @Override
    public Map<Object, Object> getHash(String key) {
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        return ops.entries(key);
    }
    @Override
    public void deleteFieldFromHash(String key, String field) {
        redisTemplate.opsForHash().delete(key, field);
    }
}
