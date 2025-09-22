package com.project.syncly.domain.chat.exception;

import com.project.syncly.global.apiPayload.exception.CustomException;

public class ChatException extends CustomException {
    public ChatException(ChatErrorCode errorCode) {
        super(errorCode);
    }
}
