package com.project.syncly.domain.auth.service;

import com.project.syncly.domain.member.dto.request.MemberRequestDTO;
import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.global.jwt.dto.IssuedTokens;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;


public interface AuthService {

    public IssuedTokens login(MemberRequestDTO.Login dto, HttpServletRequest request, HttpServletResponse response);
    String extractDeviceIdFromCookie(HttpServletRequest request);

    IssuedTokens issueNewTokens(Long memberId, String deviceId);
    IssuedTokens reissue(HttpServletRequest req); // 쿠키에서 REFRESH 꺼내는 헬퍼
    ResponseCookie buildRefreshCookie(String refreshToken, long ttlSec);


    public void logout(HttpServletRequest request, HttpServletResponse response);

}
