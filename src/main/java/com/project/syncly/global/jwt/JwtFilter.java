package com.project.syncly.global.jwt;

import com.project.syncly.domain.member.exception.MemberErrorCode;
import com.project.syncly.domain.member.exception.MemberException;
import com.project.syncly.global.apiPayload.CustomResponse;
import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import com.project.syncly.global.apiPayload.exception.CustomException;
import com.project.syncly.global.jwt.exception.JwtErrorCode;
import com.project.syncly.global.jwt.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String accessToken = extractAccessToken(request);

            if (StringUtils.hasText(accessToken)) {
                if (jwtProvider.isValid(accessToken)) {
                    processValidAccessToken(accessToken);
                    filterChain.doFilter(request, response);
                    return;
                } else {
                    request.setAttribute("exception", JwtErrorCode.INVALID_TOKEN);
                }
            } else {
                request.setAttribute("exception", JwtErrorCode.EMPTY_TOKEN);
            }

            Optional<Cookie> refreshTokenCookie = extractRefreshTokenCookie(request);
            if (refreshTokenCookie.isPresent()) {
                String refreshToken = refreshTokenCookie.get().getValue();

                if (jwtProvider.isValid(refreshToken)) {
                    String email = jwtProvider.getEmail(refreshToken);
                    Long memberId = jwtProvider.getMemberId(refreshToken);
                    UserDetails userDetails = principalDetailsService.loadUserByUsername(email);
                    String newAccessToken = tokenService.generateNewAccessToken(memberId, email);
                    response.setHeader("Authorization", "Bearer " + newAccessToken);
                    Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    tokenService.removeRefreshTokenCookie(response);
                    request.setAttribute("exception", JwtErrorCode.EXPIRED_TOKEN);
                }
            }

            filterChain.doFilter(request, response);
        } catch (CustomException e) {
            handleException(response, e);
        }
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
            throw new MemberException(MemberErrorCode.MEMBER_NOT_FOUND);
        }
    }
    
    private void handleException(HttpServletResponse response, CustomException e) throws IOException {
        BaseErrorCode code = e.getCode();
        response.setStatus(code.getStatus().value());
        response.setContentType("application/json; charset=UTF-8");

        CustomResponse<Object> customResponse = CustomResponse.failure(code.getCode(),code.getMessage());

        ObjectMapper om = new ObjectMapper();
        om.writeValue(response.getOutputStream(), customResponse);
    }
}