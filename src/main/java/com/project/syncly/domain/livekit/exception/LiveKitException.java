package com.project.syncly.domain.livekit.exception;

import com.project.syncly.global.apiPayload.exception.CustomException;

public class LiveKitException extends CustomException {
    public LiveKitException(LiveKitErrorCode errorCode) {
        super(errorCode);
    }
}
