package com.project.syncly.global.reissueFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Set;
@Slf4j
@Component
public class CsrfFilter extends OncePerRequestFilter {
    private static final Set<String> TRUSTED_ORIGINS = Set.of(
            "https://turn.syncly-io.com",
            "https://livekit.syncly-io.com",
            "https://syncly-io.com",
            "http://localhost:5173",   // dev
            "http://localhost:8080"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        final String m = req.getMethod();
        // 프리플라이트/안전메서드 스킵
        if ("OPTIONS".equalsIgnoreCase(m)) return true;
        if ("GET".equalsIgnoreCase(m) || "HEAD".equalsIgnoreCase(m)) return true;

        // 쿠키가 없으면 스킵 (stateless 요청은 CSRF 검사 안 함)
        // 헤더 체크가 가장 간단: Cookie 헤더 존재 여부
        return req.getHeader("Cookie") == null;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        // Origin/Referer 검증
        String origin = req.getHeader("Origin");
        String referer = req.getHeader("Referer");
        boolean originOk  = origin  != null && TRUSTED_ORIGINS.contains(origin);
        boolean refererOk = false;
        if (referer != null) {
            try {
                URI r = URI.create(referer);
                String schemeHostPort = r.getScheme() + "://" + r.getHost() + (r.getPort() > 0 ? ":" + r.getPort() : "");
                refererOk = TRUSTED_ORIGINS.contains(schemeHostPort);
            } catch (IllegalArgumentException ignored) {}
        }
        if (!(originOk || refererOk)) {
            log.warn("CSRF Filter 실패: origin={} referer={}", origin, referer);
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("""
        {"isSuccess":false,"code":"CSRF403","message":"사용할 수 없는 Origin/Referer 입니다"}
    """);
            return;
        }
        chain.doFilter(req, res);
    }
}
