package com.project.syncly.domain.url.exception;

import com.project.syncly.global.apiPayload.exception.CustomException;

public class UrlException extends CustomException {
    public UrlException(UrlErrorCode errorCode) {
        super(errorCode);
    }
}
