package com.project.syncly.global.jwt.exception;

import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum JwtErrorCode implements BaseErrorCode {

    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "JWT401_01", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT401_02", "만료된 토큰입니다."),
    UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT401_03", "지원되지 않는 토큰입니다."),
    EMPTY_TOKEN(HttpStatus.UNAUTHORIZED, "JWT401_04", "토큰이 비어있습니다."),
    BLACKLISTED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "JWT401_05", "블랙리스트 처리된 access token입니다."),
    BLACKLISTED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "JWT401_06", "블랙리스트 처리된 refresh token입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "JWT401_07", "인증이 필요합니다."), // fallback 용도
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "JWT403_01", "접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
