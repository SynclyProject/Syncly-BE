package com.project.syncly.global.jwt;


import com.project.syncly.domain.auth.blacklist.TokenBlacklistService;
import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.global.config.JwtConfig;
import com.project.syncly.global.jwt.enums.TokenType;
import com.project.syncly.global.jwt.exception.JwtErrorCode;
import com.project.syncly.global.jwt.exception.JwtException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final SecretKey secret;
    private final JwtConfig jwtConfig;
    private final TokenBlacklistService tokenBlacklistService;
    private final PrincipalDetailsService principalDetailsService;

    // @Value: yml에서 해당 값을 가져오기 (아래의 YML의 값을 가져올 수 있음)
    /*public JwtProvider(@Value("${Jwt.secret}") String secret,
                       @Value("${Jwt.token.access-expiration-time}") long accessExpiration,
                       @Value("${Jwt.token.refresh-expiration-time}") long refreshExpiration,
                       TokenBlacklistService tokenBlacklistService) {
        this.secret = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); // 가져온 문자열로 SecretKey 생성
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
        this.tokenBlacklistService = tokenBlacklistService;
    }*/
    public String resolveAccessToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7); // "Bearer " 이후의 토큰 문자열만 추출
        }
        return null;
    }

    // AccessToken 생성
    public String createAccessToken(Member member) {
        return createToken(member.getEmail(), member.getId(), this.jwtConfig.getAccessExpiration(), TokenType.ACCESS);
    }

    // RefreshToken 생성
    public String createRefreshToken(Member member) {
        return createToken(member.getEmail(), member.getId(), this.jwtConfig.getRefreshExpiration(), TokenType.REFRESH);
    }

    // JWT 토큰 생성 로직
    private String createToken(String email, Long memberId, long expiration, TokenType tokenType) {
        Instant issuedAt = Instant.now(); // 만들어진 시간을 현재 시간으로
        Instant expiredAt = issuedAt.plusMillis(expiration); // 만들어진 시간에 시간을 추가해 만료일 만들기

        JwtBuilder builder = Jwts.builder()
                .setHeader(Map.of("alg", "HS256", "typ", "JWT")) // JWT header 설정
                .setSubject(email) // JWT의 Subject을 email로 설정
                .setIssuedAt(Date.from(issuedAt)) // 만들어진 시간을 현재 시간으로 설정
                .setExpiration(Date.from(expiredAt)) // 유효기간 설정
                .claim("type", tokenType.name())
                .signWith(secret, SignatureAlgorithm.HS256); // 암호화를 위한 sign 설정

        if (memberId != null) {
            builder.claim("id", memberId); // id 를 Claim으로 추가
        }

        return builder.compact(); // 최종 JWT 토큰 생성
    }



    // Claim 추출
    public Jws<Claims> getClaims(String token) {
        try {
            return Jwts.parser() // JwtParserBuilder 인스턴스 반환
                    .verifyWith(secret) // 서명 검증을 위한 키 설정
                    .build() // JwtParser 인스턴스 생성
                    .parseSignedClaims(token); // 토큰 파싱 및 클레임 추출
        } catch (ExpiredJwtException e) {
            throw new JwtException(JwtErrorCode.EXPIRED_TOKEN);
        } catch (Exception e) { // parsing하는 과정에서 sign key가 틀리는 등의 이유로 일어나는 Exception
            throw new JwtException(JwtErrorCode.INVALID_TOKEN);
        }
    }

    // Email(subject) 추출
    public String getEmail(String token) {
        return getClaims(token).getBody().getSubject();
    }

    // memberId claim 추출
    public Long getMemberIdWithBlacklistCheck(String token, TokenType tokenType) {
        switch (tokenType) {
            case ACCESS:
                if (tokenBlacklistService.isAccessTokenBlacklisted(token)) {
                    throw new JwtException(JwtErrorCode.BLACKLISTED_ACCESS_TOKEN);
                }
                break;
            case REFRESH:
                if (tokenBlacklistService.isRefreshTokenBlacklisted(token)) {
                    throw new JwtException(JwtErrorCode.BLACKLISTED_REFRESH_TOKEN);
                }
                break;
            default:
                throw new JwtException(JwtErrorCode.UNSUPPORTED_TOKEN);
        }

        return getMemberId(token); // 내부는 순수 파싱
    }


    public Long getMemberId(String token) {
        return getClaims(token).getBody().get("id", Long.class);
    }

    // 만료까지 남은 시간
    public long getRemainTime(String token) {
        Date expiration = getClaims(token).getBody().getExpiration();
        return Math.max(expiration.getTime() - new Date().getTime(), 0);
    }

    // HttpOnly Secure 쿠키 생성
    public ResponseCookie createCookie(String name, String value) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true) // https 환경에서만 쿠키 전송
                .sameSite("None") // 크로스 도메인 허용
                .path("/")
                .maxAge(this.jwtConfig.getRefreshExpiration() / 1000) // 초 단위
                .build();
    }

    public ResponseCookie deleteCookie(String name) {
        return ResponseCookie.from(name, null)
                .httpOnly(true)
                .secure(true) // https 환경에서만 쿠키 전송
                .sameSite("None") // 크로스 도메인 허용
                .path("/")
                .maxAge(0) // 초 단위
                .build();
    }

    public TokenType getTokenType(String token) {
        String type = getClaims(token).getBody().get("type", String.class);
        return TokenType.from(type);
    }

    //토큰 검증
    public boolean isValidToken(String token) {
        try {
            getClaims(token); // 파싱 시도 (예외 발생 시 catch)
            return true;
        } catch (JwtException e) {
            return false;
        }
    }


    public Authentication getAuthentication(String token) {
        Long id = getMemberIdWithBlacklistCheck(token, getTokenType(token));
        UserDetails userDetails = principalDetailsService.loadUserById(id);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

}