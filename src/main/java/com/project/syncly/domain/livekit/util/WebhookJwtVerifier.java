package com.project.syncly.domain.livekit.util;

import com.project.syncly.global.config.LiveKitProperties;
import io.livekit.server.WebhookReceiver;
import org.springframework.stereotype.Component;

@Component
public class WebhookJwtVerifier {
    private final WebhookReceiver receiver;

    public WebhookJwtVerifier(LiveKitProperties properties) {
        this.receiver = new WebhookReceiver(
                properties.getWebhook().getKey(),
                properties.getWebhook().getSecret()
        );
    }

    public WebhookReceiver getReceiver() {
        return receiver;
    }
}
