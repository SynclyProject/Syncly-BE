package com.project.syncly.global.jwt.exception;

import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum JwtErrorCode implements BaseErrorCode {

    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "J001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "J002", "만료된 토큰입니다."),
    UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "J003", "지원되지 않는 토큰입니다."),
    EMPTY_TOKEN(HttpStatus.UNAUTHORIZED, "J004", "토큰이 비어있습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "J005", "접근 권한이 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "J006", "인증이 필요합니다."), // fallback 용도
    BLACKLISTED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "J007", "블랙리스트 처리된 access token입니다."),
    BLACKLISTED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "J008", "블랙리스트 처리된 refresh token입니다."),
    ;
    private final HttpStatus status;
    private final String code;
    private final String message;
}