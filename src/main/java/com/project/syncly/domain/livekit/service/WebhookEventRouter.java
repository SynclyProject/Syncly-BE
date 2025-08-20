package com.project.syncly.domain.livekit.service;

import com.project.syncly.domain.livekit.exception.LiveKitErrorCode;
import com.project.syncly.domain.livekit.exception.LiveKitException;
import com.project.syncly.domain.livekit.service.webhook.WebhookEventHandler;
import livekit.LivekitWebhook.WebhookEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookEventRouter {

    private final List<WebhookEventHandler> handlers;//스트레티지 패턴, 클래스 리스트도 자동 주입

    public void handle(WebhookEvent event) {
        handlers.stream()
                .filter(handler -> handler.supports(event))
                .findFirst()
                .ifPresentOrElse(
                        handler -> handler.handle(event),
                        () -> {
                            throw new LiveKitException(LiveKitErrorCode.UNSUPPORTED_EVENT_TYPE);
                        }
                );
    }
}