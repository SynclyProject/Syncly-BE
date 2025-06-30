package com.project.syncly.global.redis.error;

import com.project.syncly.global.apiPayload.exception.CustomException;

import java.io.Serializable;

public class RedisException extends CustomException {
    public RedisException(RedisErrorCode errorCode) {
        super(errorCode);
    }
}
