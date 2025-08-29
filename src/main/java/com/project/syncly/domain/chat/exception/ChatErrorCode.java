package com.project.syncly.domain.chat.exception;

import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatErrorCode implements BaseErrorCode {

    //채팅 관련 에러
    WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "Workspace404_0", "워크스페이스가 존재하지 않습니다."); //예시

    private final HttpStatus status;
    private final String code;
    private final String message;
}
