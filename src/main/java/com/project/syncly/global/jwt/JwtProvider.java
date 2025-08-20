package com.project.syncly.global.jwt;


import com.project.syncly.global.config.TokenProperties;
import com.project.syncly.global.jwt.dto.IssuedTokens;
import com.project.syncly.global.jwt.dto.RefreshClaims;
import com.project.syncly.global.jwt.enums.TokenType;
import com.project.syncly.global.jwt.exception.JwtErrorCode;
import com.project.syncly.global.jwt.exception.JwtException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final PrincipalDetailsService principalDetailsService;
    private final TokenProperties props;
    private final SecretKey signingKey;

    private static final String CLAIM_TYP = "typ"; // TokenType.name()
    private static final String CLAIM_DID = "did"; // deviceId
    private static final String CLAIM_JTI = "jti"; // refresh 회전용

    // Claim 추출
    private Claims parse(String jwt) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new JwtException(JwtErrorCode.EXPIRED_TOKEN);
        } catch (JwtException e) {
            throw e; // 아래 general catch로 흡수되지 않도록
        } catch (Exception e) {
            throw new JwtException(JwtErrorCode.INVALID_TOKEN);
        }
    }
    public RefreshClaims parseAndValidateRefresh(String refreshJwt) {
        Claims c = parse(refreshJwt);

        // TokenType 체크
        TokenType tokenType = TokenType.from(c.get(CLAIM_TYP, String.class));
        if (tokenType != TokenType.REFRESH) {
            throw new JwtException(JwtErrorCode.TOKEN_TYPE_MISMATCH);
        }

        Date exp = c.getExpiration();

        String sub = c.getSubject(); // userId 문자열
        String did = c.get(CLAIM_DID, String.class);
        String jti = c.get(CLAIM_JTI, String.class);

        if (sub == null || did == null || jti == null) {
            throw new JwtException(JwtErrorCode.MISSING_CLAIMS);
        }

        long userId;
        try {
            userId = Long.parseLong(sub);
        } catch (NumberFormatException e) {
            throw new JwtException(JwtErrorCode.INVALID_SUB_FORMAT);
        }

        return new RefreshClaims(userId, did, jti, exp.toInstant());
    }

    public String newJti() {
        return UUID.randomUUID().toString();
    }

    public IssuedTokens issueNewTokens(long userId, String deviceId, String newJti) {
        Instant now = Instant.now();
        Instant rtExp = now.plusSeconds(props.getRefreshTtlSec());
        return issueTokens(userId, deviceId, newJti, rtExp);
    }

    public IssuedTokens issueTokens(long userId, String deviceId, String newJti, Instant rtExp) {
        Instant now = Instant.now();
        if (!rtExp.isAfter(now)) {
            // 만료시각이 현재와 같거나 과거면 거절
            throw new JwtException(JwtErrorCode.EXPIRED_TOKEN);
        }

        // Access Token
        Instant atExp = now.plusSeconds(props.getAccessTtlSec());
        String access = Jwts.builder()
                .issuer(props.getIssuer())
                .subject(Long.toString(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(atExp))
                .claim(CLAIM_TYP, TokenType.ACCESS.name())
                .claim(CLAIM_DID, deviceId)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();

        // Refresh Token
        String refresh = Jwts.builder()
                .issuer(props.getIssuer())
                .subject(Long.toString(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(rtExp))
                .claim(CLAIM_TYP, TokenType.REFRESH.name())
                .claim(CLAIM_DID, deviceId)
                .claim(CLAIM_JTI, newJti)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();

        long atTtl = props.getAccessTtlSec();
        long rtTtl = Math.max(0, Duration.between(now, rtExp).getSeconds());

        return new IssuedTokens(access, refresh, atTtl, rtTtl);
    }


    public Long getMemberId(String token) {
        Claims c = parse(token);
        String sub = c.getSubject();
        try {
            return Long.parseLong(sub);
        } catch (NumberFormatException e) {
            throw new JwtException(JwtErrorCode.INVALID_SUB_FORMAT);
        }
    }


    public TokenType getTokenType(String token) {
        String type = parse(token).get(CLAIM_TYP, String.class);
        return TokenType.from(type);
    }

    public String getDeviceId(String jwt) {
        return parse(jwt).get(CLAIM_DID, String.class);
    }

    public Instant getExpiration(String jwt) {
        return parse(jwt).getExpiration().toInstant();
    }
    //토큰 검증
    public boolean isValidToken(String token) {
        try {
            parse(token); // 파싱 시도 (예외 발생 시 catch)
            return true;
        } catch (JwtException e) {
            return false;
        }
    }


    public Authentication getAuthentication(String token) {
        Long id = getMemberId(token);
        UserDetails userDetails = principalDetailsService.loadUserById(id);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

}