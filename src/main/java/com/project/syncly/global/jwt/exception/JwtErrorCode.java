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
    REFRESH_TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "JWT401_06", "사용된 토큰 재요청으로 블랙리스트 처리되었습니다."),

    EMPTY_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "JWT401_07", "쿠키에 refresh token이 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "JWT403_01", "접근 권한이 없습니다."),

    TOKEN_TYPE_MISMATCH(HttpStatus.UNAUTHORIZED, "JWT401_08", "토큰 타입이 올바르지 않습니다."),
    MISSING_CLAIMS(HttpStatus.UNAUTHORIZED, "JWT401_09", "필수 클레임이 누락되었습니다."),
    INVALID_SUB_FORMAT(HttpStatus.UNAUTHORIZED, "JWT401_10", "sub 클레임이 올바르지 않습니다."),

    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
