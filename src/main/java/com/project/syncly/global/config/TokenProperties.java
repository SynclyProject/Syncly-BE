package com.project.syncly.global.config;

import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

// auth.token.* 를 yml에 넣어주세요 (아래 예시 참고)
@Configuration
@ConfigurationProperties(prefix = "auth.token")
@Getter
@Setter
public class TokenProperties {
    private String issuer;
    // Base64 인코딩된 랜덤 시크릿(256비트 이상 권장)
    private String hmacSecretBase64;

    // 초 단위 권장 (AT: 600, RT: 1209600=14일)
    private long accessTtlSec;
    private long refreshTtlSec;

    @Bean
    public SecretKey jwtSecretKey() {
        return Keys.hmacShaKeyFor(hmacSecretBase64.getBytes(StandardCharsets.UTF_8));
    }

}
