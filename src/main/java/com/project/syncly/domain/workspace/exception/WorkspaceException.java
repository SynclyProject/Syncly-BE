package com.project.syncly.domain.workspace.exception;

import com.project.syncly.global.apiPayload.exception.CustomException;

public class WorkspaceException extends CustomException {
    public WorkspaceException(WorkspaceErrorCode errorCode) {
        super(errorCode);
    }
}
