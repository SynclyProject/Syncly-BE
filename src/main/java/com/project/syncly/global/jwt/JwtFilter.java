package com.project.syncly.global.jwt;

import com.project.syncly.domain.member.exception.MemberErrorCode;
import com.project.syncly.domain.member.exception.MemberException;
import com.project.syncly.global.jwt.exception.JwtErrorCode;
import com.project.syncly.global.jwt.exception.JwtException;
import com.project.syncly.global.jwt.service.TokenService;
import com.project.syncly.domain.auth.blacklist.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final PrincipalDetailsService principalDetailsService;
    private final TokenService tokenService;
    private final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String accessToken = extractAccessToken(request);

        if (StringUtils.hasText(accessToken)) {
            try {
                jwtProvider.getClaims(accessToken); // 검증 실패 시 예외 발생
                /// 블랙리스트 검사
                if (tokenBlacklistService.isAccessTokenBlacklisted(accessToken)) {
                    throw new JwtException(JwtErrorCode.BLACKLISTED_ACCESS_TOKEN);
                }

                processValidAccessToken(accessToken);
                filterChain.doFilter(request, response);//accessToken있다면 반환하고 JwtFilter 끝
                return;
            } catch (JwtException e) {
                request.setAttribute("exception", e.getCode()); // 예외만 넘김
            }
        } else {
            request.setAttribute("exception", JwtErrorCode.EMPTY_TOKEN);
        }


        Optional<Cookie> refreshTokenCookie = extractRefreshTokenCookie(request);
        if (refreshTokenCookie.isPresent()) {
            String refreshToken = refreshTokenCookie.get().getValue();
            try {
                jwtProvider.getClaims(refreshToken);
                /// 블랙리스트 검사
                if (tokenBlacklistService.isRefreshTokenBlacklisted(refreshToken)) {
                    throw new JwtException(JwtErrorCode.BLACKLISTED_REFRESH_TOKEN);
                }

                String email = jwtProvider.getEmail(refreshToken);
                Long memberId = jwtProvider.getMemberIdWithBlacklistCheck(refreshToken);
                UserDetails userDetails = principalDetailsService.loadUserByUsername(email);

                String newAccessToken = tokenService.reissueAccessToken(memberId, response);
                response.setHeader("Authorization", "Bearer " + newAccessToken);

                Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                //재발급 성공했으면 예외 삭제
                request.removeAttribute("exception");
            } catch (JwtException e) {
                tokenService.removeRefreshTokenCookie(response);

                if (e.getCode() == JwtErrorCode.INVALID_TOKEN) {
                    // 토큰 변조 가능성 있으므로 블랙리스트 추가
                    tokenBlacklistService.blacklistRefreshToken(refreshToken);
                }
                request.setAttribute("exception", e.getCode());
            }
        }else {
            // refresh token 자체가 없으면 예외 전달
            request.setAttribute("exception", JwtErrorCode.EMPTY_TOKEN);
        }

        filterChain.doFilter(request, response); //무조건 마지막엔 호출
    }

    private String extractAccessToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private Optional<Cookie> extractRefreshTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                    .findFirst();
        }
        return Optional.empty();
    }

    private void processValidAccessToken(String token) {
        String email = jwtProvider.getEmail(token);
        UserDetails userDetails = principalDetailsService.loadUserByUsername(email);

        if (userDetails != null) {
            Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            // 이 부분만은 인증 실패로 간주되어야 하므로 EXCEPTION 설정
            throw new MemberException(MemberErrorCode.MEMBER_NOT_FOUND);
        }
    }
}
