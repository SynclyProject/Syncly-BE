package com.project.syncly.domain.livekit.service.webhook;

import livekit.LivekitWebhook.WebhookEvent;

public interface WebhookEventHandler {
    boolean supports(WebhookEvent event);
    void handle(WebhookEvent event);
}
