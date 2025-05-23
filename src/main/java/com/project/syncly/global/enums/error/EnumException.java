package com.project.syncly.global.enums.error;

import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import com.project.syncly.global.apiPayload.exception.CustomException;

public class EnumException extends CustomException {
    public EnumException(BaseErrorCode code) {
        super(code);
    }
}
