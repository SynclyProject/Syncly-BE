package com.project.syncly.global.redis.error;

import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import org.springframework.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
@Getter
@AllArgsConstructor
public enum RedisErrorCode implements BaseErrorCode {

    CONVERT_FAIL_REDIS_TO_OBJECT(HttpStatus.INTERNAL_SERVER_ERROR, "REDIS_500_01", "Redis 값에서 객체로의 변환에 실패했습니다"),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
