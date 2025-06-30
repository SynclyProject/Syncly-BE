package com.project.syncly.global.enums.error;


import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EnumErrorCode implements BaseErrorCode {
    INVALID_ENUM_KEY(HttpStatus.BAD_REQUEST, "ENUM_001", "유효하지 않은 Enum 값입니다."),
    ENUM_NOT_FOUND(HttpStatus.BAD_REQUEST, "ENUM_002","변환할 타입이 Enum이 아닙니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}