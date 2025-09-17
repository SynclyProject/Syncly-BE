package com.project.syncly.domain.file.exception;

import com.project.syncly.global.apiPayload.exception.CustomException;

public class FileException extends CustomException {
    public FileException(FileErrorCode errorCode) {
        super(errorCode);
    }
}