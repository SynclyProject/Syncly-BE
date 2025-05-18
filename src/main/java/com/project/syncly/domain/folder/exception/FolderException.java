package com.project.syncly.domain.folder.exception;

import com.project.syncly.global.apiPayload.exception.CustomException;

public class FolderException extends CustomException {
    public FolderException(FolderErrorCode errorCode) {
        super(errorCode);
    }
}
