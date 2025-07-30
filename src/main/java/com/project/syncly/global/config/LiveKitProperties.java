package com.project.syncly.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "livekit")
public class LiveKitProperties {
    private Ingress ingress;
    private Admin admin;
    private Webhook webhook;
    //서버입장 jwt생성용
    @Getter @Setter
    public static class Ingress {
        private String apiKey;
        private String apiSecret;
    }
    //방관리 jwt 생성용
    @Getter @Setter
    public static class Admin {
        private String apiKey;
        private String apiSecret;
    }
    //webhook jwt verify
    @Getter @Setter
    public static class Webhook {
        private String key;
        private String secret;
    }
}
