package com.project.syncly.global.config;



import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Getter
@Configuration
public class JwtConfig {

    @Value("${Jwt.secret}")
    private String jwtSecret;

    @Value("${Jwt.token.access-expiration-time}")
    private Long accessExpiration;

    @Value("${Jwt.token.refresh-expiration-time}")
    private Long refreshExpiration;

    @Bean
    public SecretKey jwtSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

}
