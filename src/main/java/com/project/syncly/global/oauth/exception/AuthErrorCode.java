package com.project.syncly.global.oauth.exception;

import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    OAUTH_LOGIN_FAIL(HttpStatus.UNAUTHORIZED, "AUTH_001", "소셜 로그인에 실패했습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "유효하지 않은 인증 토큰입니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "AUTH_003", "권한이 없습니다."),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH_004", "지원하지 않는 OAuth Provider입니다.");
    ;
    private final HttpStatus status;
    private final String code;
    private final String message;
}