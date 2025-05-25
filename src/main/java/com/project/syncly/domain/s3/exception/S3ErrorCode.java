package com.project.syncly.domain.s3.exception;

import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
@Getter
@AllArgsConstructor
public enum S3ErrorCode implements BaseErrorCode {

    OBJECT_KEY_NOT_FOUND(HttpStatus.BAD_REQUEST, "S3_400_01", "유효하지 않은 업로드 요청입니다."),
    OBJECT_KEY_MISMATCH(HttpStatus.BAD_REQUEST, "S3_400_02", "요청된 objectKey 값이 일치하지 않습니다."),
    DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3_500_01", "S3 객체 삭제에 실패했습니다."),
    PRESIGNED_URL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3_500_02", "Presigned URL 생성에 실패했습니다."),
    UNSUPPORTED_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "S3_400_03", "지원하지 않는 파일 확장자입니다."),
    UNSUPPORTED_MIME_TYPE(HttpStatus.BAD_REQUEST, "S3_400_04", "지원하지 않는 MIME 타입입니다."),
    MIME_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "S3_400_05", "파일의 확장자가 MIME 타입과 일치하지 않습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
