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
import java.util.Map;

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

        Map<String, Object> errorResult = Map.of(
                "action", "ERROR",
                "details", ex.getCode().getMessage()
        );


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


    @MessageExceptionHandler(Exception.class)
    public void handleAllOtherExceptions(Exception ex, Principal principal) {
        log.error("[ WebSocket Unexpected Error ]: {}", ex.getMessage());

        Map<String, Object> errorResult = Map.of(
                "action", "ERROR",
                "details", "알 수 없는 서버 에러가 발생했습니다."
        );

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

}

