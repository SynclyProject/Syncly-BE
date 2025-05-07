package com.project.syncly.domain.auth.blacklist;


import com.project.syncly.global.jwt.JwtProvider;
import com.project.syncly.global.jwt.exception.JwtErrorCode;
import com.project.syncly.global.jwt.exception.JwtException;
import com.project.syncly.global.redis.core.RedisStorage;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final SecretKey secret;
    private final RedisStorage redisStorage;

    public void blacklistAccessToken(String token) {
        Duration ttl = getValidTtlOrNull(token);
        if (ttl == null) return;

        String key = RedisKeyPrefix.BLACKLIST_ACCESS.get(token);

        redisStorage.set(key, "true", ttl);
        log.info("[BLACKLIST] AccessToken 등록 완료");
    }

    public void blacklistRefreshToken(String token) {
        Duration ttl = getValidTtlOrNull(token);
        if (ttl == null) return;

        String key = RedisKeyPrefix.BLACKLIST_REFRESH.get(token);

        redisStorage.set(key, "true", ttl);
        log.info("[BLACKLIST] RefreshToken 등록 완료");
    }

    public boolean isAccessTokenBlacklisted(String token) {
        String key = RedisKeyPrefix.BLACKLIST_ACCESS.get(token);
        return "true".equals(redisStorage.get(key));
    }

    public boolean isRefreshTokenBlacklisted(String token) {
        String key = RedisKeyPrefix.BLACKLIST_REFRESH.get(token);
        return "true".equals(redisStorage.get(key));
    }

    private Duration getValidTtlOrNull(String token) {
        if (!StringUtils.hasText(token)) return null;
        long remainingMillis = extractRemainingMillis(token);
        if (remainingMillis <= 0) return null;
        return Duration.ofMillis(remainingMillis);
    }
    private long extractRemainingMillis(String token) {
        try {
            Jws<Claims> claims = Jwts.parser()
                    .verifyWith(secret)
                    .build()
                    .parseSignedClaims(token);
            Date expiration = claims.getPayload().getExpiration();
            return Math.max(expiration.getTime() - System.currentTimeMillis(), 0);
        } catch (ExpiredJwtException e) {
            throw new JwtException(JwtErrorCode.EXPIRED_TOKEN); // 토큰 만료 시도 명시
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException(JwtErrorCode.INVALID_TOKEN); // 파싱 자체 실패
        }
    }

}
