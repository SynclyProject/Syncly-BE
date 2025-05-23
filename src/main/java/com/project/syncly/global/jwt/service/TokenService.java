package com.project.syncly.global.jwt.service;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.service.MemberQueryService;
import com.project.syncly.global.jwt.JwtProvider;
import com.project.syncly.global.jwt.exception.JwtErrorCode;
import com.project.syncly.global.jwt.exception.JwtException;
import com.project.syncly.domain.auth.cache.LoginCacheService;
import com.project.syncly.domain.auth.blacklist.TokenBlacklistService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProvider jwtProvider;
    private final MemberQueryService memberQueryService;
    private final TokenBlacklistService tokenBlacklistService;
    private final LoginCacheService loginCacheService;

    @Value("${Jwt.token.refresh-expiration-time}")
    private long refreshTokenExpirationTime;

    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    // 로그인 시 토큰 발급
    public String issueTokens(Member member, HttpServletResponse response) {
        String accessToken = jwtProvider.createAccessToken(member);
        String refreshToken = jwtProvider.createRefreshToken(member);

        // 1. Access Token → Authorization 헤더에 추가
        response.setHeader("Authorization", "Bearer " + accessToken);

        // 2. Refresh Token → HttpOnly Secure 쿠키에 저장
        addRefreshTokenToCookie(response, refreshToken);

        return accessToken; // 바디에도 포함할 수 있지만, 보통 헤더로 충분함
    }

    // [JWT Filter] Access Token 재발급
    public String reissueAccessToken(Long memberId, HttpServletResponse response) {
        Member member = memberQueryService.getMemberById(memberId);
        String newAccessToken = jwtProvider.createAccessToken(member);
        response.setHeader("Authorization", "Bearer " + newAccessToken);
        return newAccessToken;
    }

    // Refresh Token 쿠키에서 추출
    public String extractRefreshToken(HttpServletRequest request) {
        return Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                .filter(c -> REFRESH_COOKIE_NAME.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new JwtException(JwtErrorCode.EMPTY_TOKEN));
    }

    // 로그아웃 처리
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);

        tokenBlacklistService.blacklistRefreshToken(refreshToken);//블랙리스트 올리기
        removeRefreshTokenCookie(response);
    }

    // 쿠키에 리프레시 토큰 저장
    private void addRefreshTokenToCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);          // JavaScript로 접근 불가능
        cookie.setSecure(true);            // HTTPS로만 전송
        cookie.setPath("/");               // 모든 경로에서 쿠키 사용 가능
        cookie.setMaxAge((int) (refreshTokenExpirationTime / 1000));
        response.addCookie(cookie);
    }

    // 쿠키 삭제
    public void removeRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
