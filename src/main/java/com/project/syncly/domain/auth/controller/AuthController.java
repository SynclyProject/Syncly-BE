package com.project.syncly.domain.auth.controller;

import com.project.syncly.domain.auth.oauth.dto.AccessTokenResponse;
import com.project.syncly.domain.auth.service.AuthService;
import com.project.syncly.domain.member.dto.request.MemberRequestDTO;
import com.project.syncly.global.apiPayload.CustomResponse;
import com.project.syncly.global.jwt.dto.IssuedTokens;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<CustomResponse<String>> login(@RequestBody MemberRequestDTO.Login requestDto, HttpServletRequest request, HttpServletResponse response) {
        IssuedTokens  issued = authService.login(requestDto, request, response);
        // 쿠키 세팅
        response.addHeader("Set-Cookie", authService
                .buildRefreshCookie(issued.refreshToken(), issued.refreshExpiresInSec()).toString());

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, issued.accessToken()));
    }

    @PostMapping("/reissue")
    public ResponseEntity<CustomResponse<AccessTokenResponse>> reissue(HttpServletRequest request, HttpServletResponse response) {
        IssuedTokens issued = authService.reissue(request);

        // REFRESH 쿠키 세팅
        ResponseCookie refreshCookie = authService.buildRefreshCookie(
                issued.refreshToken(), issued.refreshExpiresInSec()
        );
        response.addHeader("Set-Cookie", refreshCookie.toString());

        AccessTokenResponse accessTokenResponse = AccessTokenResponse.builder()
                .accessToken(issued.accessToken())
                .accessExpiresInSec(issued.accessExpiresInSec())
                .build();

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, accessTokenResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<CustomResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK));
    }
}
