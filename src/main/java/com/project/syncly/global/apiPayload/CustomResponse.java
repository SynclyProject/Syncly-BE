package com.project.syncly.global.apiPayload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({"isSuccess", "status", "code", "message", "result"})
public class CustomResponse<T> {
    @JsonProperty("isSuccess")
    private boolean isSuccess;
    @JsonProperty("code")
    private String code;
    @JsonProperty("message")
    private String message;
    @JsonInclude(JsonInclude.Include.NON_NULL) //필드 값이 null 이면 JSON 응답에서 제외됨.
    @JsonProperty("result")
    private final T result;
    //기본적으로 200 OK를 사용하는 성공 응답 생성 메서드
    public static <T> CustomResponse<T> success(T result) {
        return new CustomResponse<>(true, String.valueOf(HttpStatus.OK.value()), HttpStatus.OK.getReasonPhrase(), result);
    }
    //상태 코드를 받아서 사용하는 성공 응답 생성 메서드
    public static <T> CustomResponse<T> success(HttpStatus status, T result) {
        return new CustomResponse<>(true, String.valueOf(status.value()), status.getReasonPhrase(), result);
    }
    //상태 코드를 받아서 사용하는 성공 응답 생성 메서드
    public static  CustomResponse success(HttpStatus status) {
        return new CustomResponse<>(true, String.valueOf(status.value()), status.getReasonPhrase(), null);
    }
    //실패 응답 생성 메서드 (데이터 포함)
    public static <T> CustomResponse<T> failure(String code, String message, T result) {
        return new CustomResponse<>(false, code, message, result);
    }
    //실패 응답 생성 메서드 (데이터 없음)
    public static <T> CustomResponse<T> failure(String code, String message) {
        return new CustomResponse<>(false, code, message, null);
    }

    // BaseErrorCode 기반 실패 응답
    public static CustomResponse<Void> from(BaseErrorCode errorCode) {
        return new CustomResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> CustomResponse<T> from(BaseErrorCode errorCode, T result) {
        return new CustomResponse<>(false, errorCode.getCode(), errorCode.getMessage(), result);
    }


}
