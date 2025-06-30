package com.project.syncly.domain.s3.exception;


import com.project.syncly.global.apiPayload.exception.CustomException;

public class S3Exception extends CustomException {
    public S3Exception(S3ErrorCode errorCode) {
        super(errorCode);
    }
}
