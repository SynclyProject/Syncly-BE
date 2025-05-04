package com.project.syncly.domain.workspace.exception;

import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WorkspaceErrorCode implements BaseErrorCode {
    // 사용자 관련 에러
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "Auth404_0", "존재하지 않는 유저입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}