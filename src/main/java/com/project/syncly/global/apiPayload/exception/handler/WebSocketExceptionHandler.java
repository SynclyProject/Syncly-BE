package com.project.syncly.global.apiPayload.exception.handler;

import com.project.syncly.domain.note.exception.NoteErrorCode;
import com.project.syncly.domain.note.exception.NoteException;
import com.project.syncly.global.apiPayload.CustomResponse;
import com.project.syncly.global.apiPayload.code.GeneralErrorCode;
import com.project.syncly.global.apiPayload.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class WebSocketExceptionHandler {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketExceptionHandler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * NoteException 처리
     */
    @MessageExceptionHandler(NoteException.class)
    public void handleNoteException(NoteException ex, Principal principal) {
        log.warn("[ WebSocket NoteException ]: {}", ex.getCode().getMessage());

        if (principal == null) {
            log.warn("Principal이 null입니다");
            return;
        }

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("code", ex.getCode().getCode());
        errorDetails.put("message", ex.getCode().getMessage());
        errorDetails.put("timestamp", LocalDateTime.now());

        // 클라이언트의 대응 액션 지정
        String action = determineClientAction(ex.getCode());
        errorDetails.put("action", action);

        if (ex.getCustomMessage() != null) {
            errorDetails.put("detail", ex.getCustomMessage());
        }

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                CustomResponse.failure(
                        ex.getCode().getCode(),
                        ex.getCode().getMessage(),
                        errorDetails
                )
        );
    }

    /**
     * Redis 연결 오류 처리
     */
    @MessageExceptionHandler(RedisConnectionFailureException.class)
    public void handleRedisConnectionError(RedisConnectionFailureException ex, Principal principal) {
        log.error("[ WebSocket Redis Error ]: {}", ex.getMessage());

        if (principal == null) {
            log.warn("Principal이 null입니다");
            return;
        }

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("code", NoteErrorCode.REDIS_CONNECTION_FAILED.getCode());
        errorDetails.put("message", NoteErrorCode.REDIS_CONNECTION_FAILED.getMessage());
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("action", "RETRY");  // 클라이언트는 재시도 가능

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                CustomResponse.failure(
                        NoteErrorCode.REDIS_CONNECTION_FAILED.getCode(),
                        NoteErrorCode.REDIS_CONNECTION_FAILED.getMessage(),
                        errorDetails
                )
        );
    }

    /**
     * CustomException 처리
     */
    @MessageExceptionHandler(CustomException.class)
    public void handleCustomException(CustomException ex, Principal principal) {
        log.warn("[ WebSocket CustomException ]: {}", ex.getCode().getMessage());

        if (principal == null) {
            log.warn("Principal이 null입니다");
            return;
        }

        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("code", ex.getCode().getCode());
        errorResult.put("action", "ERROR");
        errorResult.put("details", ex.getCode().getMessage());
        errorResult.put("timestamp", LocalDateTime.now());

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                CustomResponse.failure(
                        ex.getCode().getCode(),
                        ex.getCode().getMessage(),
                        errorResult
                )
        );
    }

    /**
     * 모든 예외 처리
     */
    @MessageExceptionHandler(Exception.class)
    public void handleAllOtherExceptions(Exception ex, Principal principal) {
        log.error("[ WebSocket Unexpected Error ]: {}", ex.getMessage(), ex);

        if (principal == null) {
            log.warn("Principal이 null입니다");
            return;
        }

        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("code", GeneralErrorCode.INTERNAL_SERVER_ERROR_500.getCode());
        errorResult.put("action", "ERROR");
        errorResult.put("details", "알 수 없는 서버 에러가 발생했습니다");
        errorResult.put("timestamp", LocalDateTime.now());

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                CustomResponse.failure(
                        GeneralErrorCode.INTERNAL_SERVER_ERROR_500.getCode(),
                        GeneralErrorCode.INTERNAL_SERVER_ERROR_500.getMessage(),
                        errorResult
                )
        );
    }

    /**
     * 에러 코드에 따라 클라이언트가 취해야 할 액션 결정
     *
     * <p>클라이언트는 이 액션에 따라 다음과 같이 대응합니다:
     * <ul>
     *   <li>RELOAD: 페이지 새로고침 필요 (revision 불일치, 동시 편집 충돌)</li>
     *   <li>RETRY: 작업 재시도 가능 (일시적 오류)</li>
     *   <li>RECONNECT: WebSocket 재연결 필요 (연결 오류)</li>
     *   <li>IGNORE: 무시 가능한 에러</li>
     * </ul>
     *
     * @param errorCode 에러 코드
     * @return 클라이언트 액션
     */
    private String determineClientAction(com.project.syncly.global.apiPayload.code.BaseErrorCode errorCode) {
        String code = errorCode.getCode();

        // Revision 불일치 또는 동시 편집 충돌 → 페이지 새로고침
        if (code.contains("409") || code.equals("Note409_2")) {
            return "RELOAD";
        }

        // Redis 오류 → 재시도
        if (code.contains("503") || code.equals("Note500_3")) {
            return "RETRY";
        }

        // 권한 오류 → 무시 또는 로그인 페이지
        if (code.contains("403")) {
            return "REDIRECT_LOGIN";
        }

        // 기타 오류
        return "ERROR";
    }

}

