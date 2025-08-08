package com.project.syncly.domain.livekit.exception;

import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import org.springframework.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
@Getter
@AllArgsConstructor
public enum LiveKitErrorCode  implements BaseErrorCode {

    MEMBER_NOT_INCLUDE_WORKSPACE(HttpStatus.FORBIDDEN, "WORKSPACE_MEMBER_401_01", "member가 워크스페이스에 가입되어있지 않습니다"),
    INVALID_WEBHOOK_SIGNATURE(HttpStatus.UNAUTHORIZED, "LIVEKIT_401_01", "Webhook 인증 서명이 유효하지 않습니다."),
    UNSUPPORTED_EVENT_TYPE(HttpStatus.BAD_REQUEST, "LIVEKIT_400_01", "지원하지 않는 이벤트 타입입니다."),
    INVALID_WEBHOOK_BODY(HttpStatus.BAD_REQUEST, "LIVEKIT_400_02", "Webhook 요청 바디가 유효하지 않습니다."),
    ROOM_DELETION_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "LIVEKIT_500_01", "방 삭제에 실패했습니다.");
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
