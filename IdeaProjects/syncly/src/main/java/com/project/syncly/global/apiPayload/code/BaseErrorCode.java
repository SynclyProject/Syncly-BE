package com.project.syncly.global.apiPayload.code;

import com.project.syncly.global.apiPayload.CustomResponse;
import org.springframework.http.HttpStatus;

public interface BaseErrorCode {

    HttpStatus getStatus();
    String getCode();
    String getMessage();

    default CustomResponse<Void> getErrorResponse() {
        return CustomResponse.failure(getCode(), getMessage());
    }
}
