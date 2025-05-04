package com.project.syncly.global.oauth.exception;

import com.project.syncly.global.apiPayload.exception.CustomException;

public class AuthException extends CustomException {
    public AuthException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}
