package com.project.syncly.domain.livekit.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.project.syncly.global.config.LiveKitProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LiveKitTokenUtil {

    private final LiveKitProperties liveKitProperties;

    public String createToken(String identity, String roomName) {
        Algorithm algorithm = Algorithm.HMAC256(liveKitProperties.getApiSecret());

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(3600); // 1시간

        return JWT.create()
                .withIssuer(liveKitProperties.getApiKey())
                .withSubject(identity)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(exp))
                .withJWTId(UUID.randomUUID().toString())
                .withClaim("video", Map.of(
                        "roomName", roomName,
                        "roomCreate", true,
                        "canPublish", true,
                        "canSubscribe", true
                ))
                .sign(algorithm);
    }
}