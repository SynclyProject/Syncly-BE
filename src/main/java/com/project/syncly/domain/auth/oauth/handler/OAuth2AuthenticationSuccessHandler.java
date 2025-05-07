package com.project.syncly.domain.auth.oauth.handler;


import com.project.syncly.global.jwt.JwtProvider;
import com.project.syncly.domain.auth.oauth.dto.CustomOAuth2User;
import com.project.syncly.domain.auth.oauth.dto.TokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        CustomOAuth2User user = (CustomOAuth2User) authentication.getPrincipal();
        String accessToken = jwtProvider.createAccessToken(user.getName());
        String refreshToken = jwtProvider.createRefreshToken(user.getName());

        // accessToken은 Authorization 헤더에 추가
        response.setHeader("Authorization", "Bearer " + accessToken);

        // refreshToken은 HttpOnly + Secure 쿠키로 설정
        response.setHeader("Set-Cookie", jwtProvider.createCookie("refreshToken", refreshToken).toString());

        // 클라이언트가 accessToken 만료 시간 등 필요한 추가 정보가 있다면 바디에도 전달
        TokenResponse tokenResponse = new TokenResponse(accessToken);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), tokenResponse);
    }

}