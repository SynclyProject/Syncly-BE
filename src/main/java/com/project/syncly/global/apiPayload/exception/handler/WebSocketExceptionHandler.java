package com.project.syncly.global.apiPayload.exception.handler;

import com.project.syncly.global.apiPayload.CustomResponse;
import com.project.syncly.global.apiPayload.code.GeneralErrorCode;
import com.project.syncly.global.apiPayload.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.security.Principal;

@Slf4j
@ControllerAdvice
public class WebSocketExceptionHandler {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketExceptionHandler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageExceptionHandler(CustomException.class)
    public void handleCustomException(CustomException ex, Principal principal) {
        log.warn("[ WebSocket CustomException ]: {}", ex.getCode().getMessage());

        CustomResponse<Void> response = CustomResponse.from(ex.getCode());

        messagingTemplate.convertAndSendToUser(
                principal.getName(),                // 로그인된 사용자 식별
                "/queue/errors",                    // 클라이언트에서 구독할 개인 큐
                response                       // 에러 메시지 전달
        );
    }

    @MessageExceptionHandler(Exception.class)
    public void handleAllOtherExceptions(Exception ex, Principal principal) {
        log.error("[ WebSocket Unexpected Error ]: {}", ex.getMessage());

        CustomResponse<Void> response = GeneralErrorCode.INTERNAL_SERVER_ERROR_500.getErrorResponse();

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                response
        );
    }
}

