package com.project.syncly.global.redis.core;

import java.time.Duration;

public interface RedisStorage {
    void set(String key, String value, Duration ttl);
    String get(String key);
    void delete(String key);

    // 객체 저장/조회 지원 메서드 추가
    <T> void set(String key, T value, Duration ttl);
    <T> T get(String key, Class<T> clazz);
}