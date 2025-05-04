package com.project.syncly.global.jwt.service;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.global.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    @Value("${Jwt.token.refresh-expiration-time}")
    private long refreshTokenExpirationTime;
    
    private final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    /**
     * 로그인 성공 시 Access Token과 Refresh Token을 생성하고,
     * Refresh Token은 쿠키에 저장, Access Token은 응답 헤더에 담아 반환
     */
    public String generateTokens(Member member, HttpServletResponse response) {
        // Access Token 생성
        String accessToken = jwtProvider.createAccessToken(member);
        
        // Refresh Token 생성
        String refreshToken = jwtProvider.createRefreshToken(member);
        
        // HTTP-Only, Secure 쿠키에 Refresh Token 저장
        addRefreshTokenToCookie(response, refreshToken);
        
        return accessToken;
    }
    
    /**
     * Access Token 만료 시 새로운 Access Token 발급
     */
    public String generateNewAccessToken(Long memberId, String email) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + memberId));
                
        if (!Objects.equals(member.getEmail(), email)) {
            throw new IllegalArgumentException("Token does not match member");
        }
        
        return jwtProvider.createAccessToken(member);
    }
    
    /**
     * HTTP-Only, Secure 쿠키에 Refresh Token 저장
     */
    private void addRefreshTokenToCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);          // JavaScript로 접근 불가능
        cookie.setSecure(true);            // HTTPS로만 전송
        cookie.setPath("/");               // 모든 경로에서 쿠키 사용 가능
        
        // 쿠키 유효 시간 설정 (밀리초를 초로 변환)
        int maxAge = (int) (refreshTokenExpirationTime / 1000);
        cookie.setMaxAge(maxAge);
        
        response.addCookie(cookie);
    }
    
    /**
     * Refresh Token 쿠키 삭제 (로그아웃 시)
     */
    public void removeRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);  // 즉시 만료
        
        response.addCookie(cookie);
    }
} 