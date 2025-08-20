package com.project.syncly.global.redis.core;

import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RedisStorage {

    void setValueAsString(String key, String value, Duration ttl);
    <T> void set(String key, T value, Duration ttl);

    String getValueAsString(String key);
    <T> T getValueAsString(String key, Class<T> clazz);

    void delete(String key);

    //set
    Long addToSet(String key, String value);
    Long removeFromSet(String key, String value);
    Set<String> getSetValues(String key);

    //zset
    Boolean addToZSet(String key, String value, double score);
    Long removeFromZSet(String key, String value);
    Set<String> getZSetByScoreRange(String key, double minScore, double maxScore);

    //hash
    void setHash(String key, Map<String, Object> values, Duration ttl);
    void updateHashField(String key, String field, Object value);
    Map<String, Object> getHash(String key);


    //script (원자단위로 실행하기 위함)
    <T> T executeScript(DefaultRedisScript<T> script, List<String> keys, Object... args);
}