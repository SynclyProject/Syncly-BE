package com.project.syncly.domain.auth.controller;

import com.project.syncly.domain.auth.service.AuthService;
import com.project.syncly.domain.member.dto.request.MemberRequestDTO;
import com.project.syncly.global.apiPayload.CustomResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 실 배포시엔 token 반환 삭제하기
    @PostMapping("/login")
    public ResponseEntity<CustomResponse<String>> login(@RequestBody MemberRequestDTO.Login request, HttpServletResponse response) {
        String accessToken = authService.login(request.email(), request.password(), response);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, accessToken));
    }

    /// 테스트용
    @PostMapping("/reissue")
    public ResponseEntity<CustomResponse<String>> reissue(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = authService.reissueAccessToken(request, response);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, accessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<CustomResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK));
    }
}
