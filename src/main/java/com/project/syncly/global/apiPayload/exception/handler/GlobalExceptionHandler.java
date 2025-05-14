package com.project.syncly.global.apiPayload.exception.handler;

import com.project.syncly.global.apiPayload.CustomResponse;
import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import com.project.syncly.global.apiPayload.code.GeneralErrorCode;
import com.project.syncly.global.apiPayload.exception.CustomException;
import com.project.syncly.global.jwt.exception.JwtException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 컨트롤러 메서드에서 @Valid 어노테이션을 사용하여 DTO의 유효성 검사를 수행
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<CustomResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        // 검사에 실패한 필드와 그에 대한 메시지를 저장하는 Map
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        BaseErrorCode validationErrorCode = GeneralErrorCode.VALIDATION_FAILED; // BaseErrorCode로 통일
        CustomResponse<Map<String, String>> errorResponse = CustomResponse.failure(
                validationErrorCode.getCode(),
                validationErrorCode.getMessage(),
                errors
        );
        // 에러 코드, 메시지와 함께 errors를 반환
        return ResponseEntity.status(validationErrorCode.getStatus()).body(errorResponse);
    }

    //@RequestParam, @PathVariable, @ModelAttribute 같은 메서드 파라미터 자체에 붙은 제약조건 어노테이션 위반 시
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("유효성 검증 실패 (RequestParam 등): {}", e.getMessage());
        return ResponseEntity
                .status(GeneralErrorCode.VALIDATION_FAILED.getStatus())
                .body(GeneralErrorCode.VALIDATION_FAILED.getErrorResponse());
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<?> handleJwtException(JwtException e) {
        BaseErrorCode code = e.getCode();
        log.warn("JwtException 발생: {}", code.getMessage());
        return ResponseEntity
                .status(code.getStatus())
                .body(code.getErrorResponse());
    }

    //애플리케이션에서 발생하는 커스텀 예외를 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<CustomResponse<Void>> handleCustomException(CustomException ex) {
        //예외가 발생하면 로그 기록
        log.warn("[ CustomException ]: {}", ex.getCode().getMessage());
        //커스텀 예외에 정의된 에러 코드와 메시지를 포함한 응답 제공
        return ResponseEntity.status(ex.getCode().getStatus())
                .body(ex.getCode().getErrorResponse());
    }

    // 그 외의 정의되지 않은 모든 예외 처리
    @ExceptionHandler({Exception.class})
    public ResponseEntity<CustomResponse<String>> handleAllException(Exception ex) {
        log.error("[WARNING] Internal Server Error : {} ", ex.getMessage());
        BaseErrorCode errorCode = GeneralErrorCode.INTERNAL_SERVER_ERROR_500;
        CustomResponse<String> errorResponse = CustomResponse.failure(
                errorCode.getCode(),
                errorCode.getMessage(),
                ex.getMessage()
        );
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(errorResponse);
    }
}
