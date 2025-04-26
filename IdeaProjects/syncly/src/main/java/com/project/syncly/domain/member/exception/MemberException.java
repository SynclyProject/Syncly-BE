package com.project.syncly.domain.member.exception;

import com.project.syncly.global.apiPayload.exception.CustomException;

public class MemberException extends CustomException {
    public MemberException(MemberErrorCode errorCode) {
        super(errorCode);
    }
}