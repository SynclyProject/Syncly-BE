package com.project.syncly.global.redis.core;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RedisStorage {
    void set(String key, String value, Duration ttl);
    String get(String key);
    void delete(String key);

    // 객체 저장/조회 지원 메서드 추가
    <T> void set(String key, T value, Duration ttl);
    <T> T get(String key, Class<T> clazz);

    //set
    Long addToSet(String key, String value);
    Long removeFromSet(String key, String value);
    Set<Object> getSetValues(String key);

    //zset
    Boolean addToZSet(String key, String value, double score);
    Long removeFromZSet(String key, String value);
    Set<Object> getZSetByScoreRange(String key, double minScore, double maxScore);
    Double getZSetScore(String key, String value);
    Long getZSetSize(String key);

    //hash
    void setHash(String key, Map<String, Object> values, Duration ttl);
    void updateHashField(String key, String field, Object value);
    Map<Object, Object> getHash(String key);
    void deleteFieldFromHash(String key, String field);

}