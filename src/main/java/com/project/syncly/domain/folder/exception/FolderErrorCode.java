package com.project.syncly.domain.folder.exception;

import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FolderErrorCode implements BaseErrorCode {

    FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Folder404_0", "존재하지 않는 폴더입니다."),
    INVALID_PARENT_FOLDER(HttpStatus.BAD_REQUEST, "Folder400_0", "부모 폴더 ID가 유효하지 않습니다."),
    DUPLICATE_FOLDER_NAME(HttpStatus.CONFLICT, "Folder409_0", "같은 위치에 동일한 폴더명이 이미 존재합니다."),
    WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "Folder404_1", "워크스페이스가 존재하지 않습니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "Folder403_0", "해당 폴더에 접근할 수 없습니다."),
    EMPTY_NAME(HttpStatus.BAD_REQUEST, "Folder400_1", "폴더 이름은 비워둘 수 없습니다."),
    INVALID_NAME(HttpStatus.BAD_REQUEST, "Folder400_1", "폴더 이름은 1~50자의 한글, 영문, 숫자, '-', '_'만 사용할 수 있으며 공백이나 특수문자는 허용되지 않습니다.")
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
