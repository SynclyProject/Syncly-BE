package com.project.syncly.global.apiPayload.exception.handler;

import com.project.syncly.domain.note.exception.NoteErrorCode;
import com.project.syncly.domain.note.exception.NoteException;
import com.project.syncly.global.apiPayload.CustomResponse;
import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import com.project.syncly.global.apiPayload.code.GeneralErrorCode;
import com.project.syncly.global.apiPayload.exception.CustomException;
import com.project.syncly.global.jwt.exception.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
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
        Map<String, String> errors = new HashMap<>();//이렇게 두면 에러 여러개가 들어왔을때 마지막 것만 찍히므로 MultiValueMap로 리팩토링 필요
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

    /**
     * NoteException 처리
     *
     * <p>노트 도메인에서 발생하는 비즈니스 예외를 처리합니다.
     */
    @ExceptionHandler(NoteException.class)
    public ResponseEntity<CustomResponse<Map<String, Object>>> handleNoteException(
            NoteException ex,
            HttpServletRequest request) {
        log.warn("[ NoteException ]: {}", ex.getCode().getMessage());

        Map<String, Object> details = new HashMap<>();
        details.put("code", ex.getCode().getCode());
        details.put("timestamp", LocalDateTime.now());
        details.put("path", request.getRequestURI());
        if (ex.getCustomMessage() != null) {
            details.put("detail", ex.getCustomMessage());
        }

        return ResponseEntity.status(ex.getCode().getStatus())
                .body(CustomResponse.failure(
                        ex.getCode().getCode(),
                        ex.getCode().getMessage(),
                        details
                ));
    }

    /**
     * Redis 연결 오류 처리
     *
     * <p>Redis 서버 연결 실패 시 503 Service Unavailable 반환
     */
    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<CustomResponse<Map<String, Object>>> handleRedisConnectionFailure(
            RedisConnectionFailureException ex,
            HttpServletRequest request) {
        log.error("[ Redis Connection Error ]: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        details.put("timestamp", LocalDateTime.now());
        details.put("path", request.getRequestURI());
        details.put("error", "Redis 서버에 연결할 수 없습니다");

        BaseErrorCode errorCode = NoteErrorCode.REDIS_CONNECTION_FAILED;
        return ResponseEntity.status(errorCode.getStatus())
                .body(CustomResponse.failure(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        details
                ));
    }

    /**
     * OptimisticLockingFailureException 처리
     *
     * <p>동시 편집 충돌 시 409 Conflict 반환
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<CustomResponse<Map<String, Object>>> handleOptimisticLockingFailure(
            OptimisticLockingFailureException ex,
            HttpServletRequest request) {
        log.warn("[ OptimisticLocking Failure ]: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        details.put("timestamp", LocalDateTime.now());
        details.put("path", request.getRequestURI());
        details.put("action", "RELOAD");  // 클라이언트는 페이지 새로고침 필요

        BaseErrorCode errorCode = NoteErrorCode.CONCURRENT_EDIT_CONFLICT;
        return ResponseEntity.status(errorCode.getStatus())
                .body(CustomResponse.failure(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        details
                ));
    }

    /**
     * 404 Not Found 처리
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<CustomResponse<Map<String, Object>>> handleNoHandlerFound(
            NoHandlerFoundException ex,
            HttpServletRequest request) {
        log.warn("[ 404 Not Found ]: path={}", ex.getRequestURL());

        Map<String, Object> details = new HashMap<>();
        details.put("timestamp", LocalDateTime.now());
        details.put("path", ex.getRequestURL());
        details.put("message", "요청한 리소스를 찾을 수 없습니다");

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CustomResponse.failure(
                        "404",
                        "요청한 리소스를 찾을 수 없습니다",
                        details
                ));
    }

    // 그 외의 정의되지 않은 모든 예외 처리
    @ExceptionHandler({Exception.class})
    public ResponseEntity<CustomResponse<String>> handleAllException(
            Exception ex,
            HttpServletRequest request) {
        log.error("[WARNING] Internal Server Error: path={}", request.getRequestURI(), ex);

        BaseErrorCode errorCode = GeneralErrorCode.INTERNAL_SERVER_ERROR_500;
        CustomResponse<String> errorResponse = CustomResponse.failure(
                errorCode.getCode(),
                errorCode.getMessage(),
                // TODO: 프로덕션에서는 민감한 정보 제거
                // 개발 환경에서만 실제 예외 메시지 전달
                ex.getMessage()
        );
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(errorResponse);
    }
}
