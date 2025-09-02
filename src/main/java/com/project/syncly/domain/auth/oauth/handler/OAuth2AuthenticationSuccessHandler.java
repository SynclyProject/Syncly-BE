package com.project.syncly.domain.auth.oauth.handler;


import com.project.syncly.domain.auth.service.AuthService;
import com.project.syncly.global.jwt.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.syncly.global.jwt.PrincipalDetails;
import com.project.syncly.global.jwt.dto.IssuedTokens;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;
    private final AuthService authService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        PrincipalDetails user = (PrincipalDetails) authentication.getPrincipal();
        Long userId = user.getMember().getId();
        String deviceId = authService.extractDeviceIdFromCookie(request);
        IssuedTokens issued = authService.issueNewTokens(userId,deviceId);

        ResponseCookie refreshCookie = authService.buildRefreshCookie(
                issued.refreshToken(), issued.refreshExpiresInSec()
        );
        response.addHeader("Set-Cookie", refreshCookie.toString());
        // 302는 캐시되면 안됨 → 캐시 방지
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        //response.sendRedirect("https://www.syncly-io.com/oauth2/success");
        //프론트 코드 배포 시 위 주소로 변경
        response.sendRedirect("http://localhost:5173/oauth2/success");
    }

}