package com.project.syncly.domain.url.exception;

import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UrlErrorCode implements BaseErrorCode {

    // CONNECTION 관련 에러
    USER_NOT_CONNECTED(HttpStatus.FORBIDDEN, "Conn403_0", "현재 WebSocket에 연결되어 있지 않습니다."),
    CONNECTION_EXPIRED(HttpStatus.UNAUTHORIZED, "Conn401_0", "WebSocket 연결이 만료되었습니다."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Conn404_0", "세션 정보를 찾을 수 없습니다."),
    INVALID_SESSION(HttpStatus.UNAUTHORIZED, "Conn401_1", "유효하지 않은 WebSocket 세션입니다."),

    // URL 관련 에러
    URL_TAB_NOT_FOUND(HttpStatus.NOT_FOUND, "Url404_0", "해당 URL 탭을 찾을 수 없습니다."),
    URL_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "Url404_1", "해당 URL 항목을 찾을 수 없습니다."),
    INVALID_URL_FORMAT(HttpStatus.BAD_REQUEST, "Url400_0", "유효하지 않은 URL 형식입니다."),
    URL_DUPLICATED(HttpStatus.CONFLICT, "Url409_0", "이미 등록된 URL입니다."),
    NOT_OWNER_OF_URL(HttpStatus.FORBIDDEN, "Url403_0", "해당 URL에 대한 수정 권한이 없습니다."),
    URL_TAB_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "Url400_1", "URL 탭 이름은 필수 입력값입니다."),
    URL_ITEM_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "Url400_2", "URL 항목 이름은 필수 입력값입니다."),
    URL_ITEM_LINK_REQUIRED(HttpStatus.BAD_REQUEST, "Url400_3", "URL 링크는 필수 입력값입니다."),

    // URL 탭-워크스페이스 관계 오류
    URL_TAB_NOT_BELONG_TO_WORKSPACE(HttpStatus.BAD_REQUEST, "Url400_4", "해당 URL 탭은 요청한 워크스페이스에 속하지 않습니다."),
    URL_ITEM_NOT_BELONG_TO_TAB(HttpStatus.BAD_REQUEST, "Url400_5", "해당 URL 아이템은 요청한 탭에 속하지 않습니다."),

    // Extension 관련 에러
    NO_VALID_URLS(HttpStatus.BAD_REQUEST, "Url400_6", "저장할 유효한 URL이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
