package com.project.syncly.domain.auth.service;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.exception.MemberErrorCode;
import com.project.syncly.domain.member.exception.MemberException;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.global.jwt.JwtProvider;
import com.project.syncly.global.jwt.enums.TokenType;
import com.project.syncly.global.jwt.exception.JwtErrorCode;
import com.project.syncly.global.jwt.exception.JwtException;
import com.project.syncly.global.jwt.service.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final TokenService tokenService;

    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    @Override
    public String login(String email, String password, HttpServletResponse response) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND_BY_EMAIL));

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new MemberException(MemberErrorCode.PASSWORD_NOT_MATCHED);
        }

        return tokenService.issueTokens(member, response);
    }

    @Override
    public String reissueAccessToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_COOKIE_NAME.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new JwtException(JwtErrorCode.EMPTY_TOKEN));

        TokenType tokenType = jwtProvider.getTokenType(refreshToken);
        Long memberId = jwtProvider.getMemberId(refreshToken);
        return tokenService.reissueAccessToken(memberId, response);
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        tokenService.logout(request, response);
    }
}
