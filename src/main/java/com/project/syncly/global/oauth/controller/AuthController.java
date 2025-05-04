package com.project.syncly.global.oauth.controller;


import com.project.syncly.global.apiPayload.CustomResponse;
import com.project.syncly.global.jwt.JwtProvider;
import com.project.syncly.global.jwt.exception.JwtErrorCode;
import com.project.syncly.global.jwt.exception.JwtException;
import com.project.syncly.global.jwt.service.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtProvider jwtProvider;
    private final TokenService tokenService;

    //소셜로그인 api주소
    /// oauth2/authorization/google

    //쿠키에 있는 refresh token 읽어서 검증 후 access token 발급(일반, 소셜 공통)
    @PostMapping("/reissue")
    public CustomResponse<?> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals("refreshToken"))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new JwtException(JwtErrorCode.EMPTY_TOKEN));

        if (!jwtProvider.isValid(refreshToken)) {
            throw new JwtException(JwtErrorCode.EXPIRED_TOKEN);
        }

        String email = jwtProvider.getEmail(refreshToken);
        Long memberId = jwtProvider.getMemberId(refreshToken);
        String newAccessToken = tokenService.generateNewAccessToken(memberId, email);

        // access token 헤더에 넣어줌
        response.setHeader("Authorization", "Bearer " + newAccessToken);

        // 응답 바디는 성공 메시지만
        return CustomResponse.success(HttpStatus.OK);
    }

    @PostMapping("/logout")
    public CustomResponse<?> logout(HttpServletResponse response) {
        tokenService.removeRefreshTokenCookie(response);
        return CustomResponse.success(HttpStatus.OK);
    }
}
