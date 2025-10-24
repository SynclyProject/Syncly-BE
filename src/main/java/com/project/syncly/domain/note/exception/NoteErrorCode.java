package com.project.syncly.domain.note.exception;

import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NoteErrorCode implements BaseErrorCode {

    // 노트 관련 에러
    NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "Note404_0", "노트를 찾을 수 없습니다."),
    NOTE_ALREADY_DELETED(HttpStatus.GONE, "Note410_0", "이미 삭제된 노트입니다."),

    // 권한 관련 에러
    NOTE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Note403_0", "노트에 접근할 권한이 없습니다."),
    NOT_NOTE_CREATOR(HttpStatus.FORBIDDEN, "Note403_1", "노트 작성자가 아닙니다."),

    // 유효성 검증 에러
    INVALID_NOTE_TITLE(HttpStatus.BAD_REQUEST, "Note400_0", "노트 제목이 유효하지 않습니다."),
    EMPTY_NOTE_TITLE(HttpStatus.BAD_REQUEST, "Note400_1", "노트 제목은 비워둘 수 없습니다."),
    NOTE_TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "Note400_2", "노트 제목은 최대 200자까지 입력 가능합니다."),
    NOTE_CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "Note400_3", "노트 내용이 너무 깁니다. (최대 1MB)"),

    // 워크스페이스 관련 에러
    WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "Note404_1", "워크스페이스를 찾을 수 없습니다."),
    NOT_WORKSPACE_MEMBER(HttpStatus.FORBIDDEN, "Note403_2", "워크스페이스의 멤버가 아닙니다."),

    // 참여자 관련 에러
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "Note404_2", "노트 참여자를 찾을 수 없습니다."),
    ALREADY_PARTICIPANT(HttpStatus.CONFLICT, "Note409_0", "이미 노트에 참여 중입니다."),

    // 이미지 관련 에러
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "Note404_3", "이미지를 찾을 수 없습니다."),
    INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "Note400_4", "지원하지 않는 이미지 형식입니다."),
    IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "Note400_5", "이미지 크기가 제한을 초과했습니다. (최대 10MB)"),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Note500_0", "이미지 업로드에 실패했습니다."),

    // 동시 편집 관련 에러
    CONCURRENT_EDIT_CONFLICT(HttpStatus.CONFLICT, "Note409_1", "동시 편집 충돌이 발생했습니다. 다시 시도해주세요."),
    OT_TRANSFORM_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Note500_1", "편집 내용 병합에 실패했습니다."),
    INVALID_OPERATION(HttpStatus.BAD_REQUEST, "Note400_6", "유효하지 않은 편집 연산입니다."),
    REVISION_MISMATCH(HttpStatus.CONFLICT, "Note409_2", "문서 버전이 일치하지 않습니다. 새로고침 후 다시 시도해주세요."),

    // WebSocket 연결 관련 에러
    WEBSOCKET_CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Note500_2", "WebSocket 연결에 실패했습니다."),
    WEBSOCKET_MESSAGE_PARSING_ERROR(HttpStatus.BAD_REQUEST, "Note400_7", "WebSocket 메시지를 처리할 수 없습니다."),

    // Redis 연결 관련 에러
    REDIS_CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "Note503_0", "Redis 서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요."),
    REDIS_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Note500_3", "Redis 작업에 실패했습니다."),

    // Rate Limiting 에러
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Note429_0", "요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
