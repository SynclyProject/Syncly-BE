package com.project.syncly.global.redis.core;


import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "redis.mode", havingValue = "off", matchIfMissing = true)//.env에 값 없으면 인메모리
public class InMemoryRedisStorageImpl implements RedisStorage {

    private final Map<String, Object> storage = new HashMap<>();
    @PostConstruct
    public void check() {
        System.out.println("인메모리 캐시 사용중");
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        storage.put(key, value);
    }

    @Override
    public String get(String key) {
        Object value = storage.get(key);
        return value instanceof String ? (String) value : null;
    }

    @Override
    public void delete(String key) {
        storage.remove(key);
    }

    @Override
    public <T> void set(String key, T value, Duration ttl) {
        storage.put(key, value);
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        Object value = storage.get(key);
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return null;
    }
}
