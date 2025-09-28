package com.project.syncly.domain.chat.exception;

import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatErrorCode implements BaseErrorCode {

    // 사용자 관련 에러
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "Chat404_0", "존재하지 않는 유저입니다."),

    // 워크스페이스 관련
    WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "Chat404_1", "워크스페이스가 존재하지 않습니다."),
    NOT_WORKSPACE_MEMBER(HttpStatus.FORBIDDEN, "Chat403_0", "워크스페이스 멤버가 아닙니다."),
    NOT_TEAM_WORKSPACE(HttpStatus.BAD_REQUEST, "Chat400_0", "팀 워크스페이스가 아닙니다."),

    // 메시지 관련
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "Chat404_2", "채팅 메시지를 찾을 수 없습니다."),
    DUPLICATE_MESSAGE(HttpStatus.CONFLICT, "Chat409_0", "이미 존재하는 메시지입니다."),
    INVALID_SEQ_RANGE(HttpStatus.BAD_REQUEST, "Chat400_1", "유효하지 않은 seq 범위입니다."),

    // 연결/상태 관련
    USER_NOT_CONNECTED(HttpStatus.BAD_REQUEST, "Chat400_2", "유저가 WebSocket에 연결되지 않았습니다."),
    SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Chat500_0", "메시지 전송에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
