package com.project.syncly.global.jwt.exception;

import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;

@Getter
public class JwtException extends RuntimeException {
    private final BaseErrorCode code;
    public JwtException(JwtErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode;
    }
}


