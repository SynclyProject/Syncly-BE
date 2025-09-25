package com.project.syncly.domain.file.exception;

import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FileErrorCode implements BaseErrorCode {

    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "File404_0", "존재하지 않는 파일입니다."),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "File400_0", "지원하지 않는 파일 형식입니다."),
    DUPLICATE_FILE_NAME(HttpStatus.CONFLICT, "File409_0", "같은 위치에 동일한 파일명이 이미 존재합니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "File403_0", "해당 파일에 접근할 수 없습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "File500_0", "파일 업로드에 실패했습니다."),
    FILE_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "File500_1", "파일 다운로드에 실패했습니다."),
    EMPTY_FILE_NAME(HttpStatus.BAD_REQUEST, "File400_2", "파일 이름은 비워둘 수 없습니다."),
    INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, "File400_3", "파일 이름은 1~100자의 한글, 영문, 숫자, '-', '_', '.'만 사용할 수 있습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "File400_4", "파일 크기가 50MB를 초과했습니다."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "File400_5", "지원하지 않는 파일 형식입니다."),
    FOLDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "File403_1", "해당 폴더에 접근할 수 없습니다."),
    INVALID_UPLOAD_REQUEST(HttpStatus.BAD_REQUEST, "File400_6", "유효하지 않은 업로드 요청입니다. 업로드 권한이 없거나 만료되었습니다."),
    MISSING_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "File400_7", "파일명에 확장자가 포함되어야 합니다. (예: 파일명.jpg)");

    private final HttpStatus status;
    private final String code;
    private final String message;
}