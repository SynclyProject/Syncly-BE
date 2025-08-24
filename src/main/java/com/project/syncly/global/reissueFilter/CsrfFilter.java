package com.project.syncly.global.reissueFilter;

import com.project.syncly.global.jwt.enums.TokenType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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

    // ([a-zA-Z0-9-]+\.) == .이 있는 패턴만 허용하여 루트모메인이 다르면 reject
    // https만 허용
    // syncly-io.com 이 최상위 도메인
    // 모든 포트 허용
    private static final String PROD_PATTERN = "^https://([a-zA-Z0-9-]+\\.)*syncly-io\\.com(:\\d+)?$";

    // 패턴으로 잡지 못하는 로컬 테스트용
    private static final Set<String> TRUSTED_ORIGINS = Set.of(
            "http://52.79.102.15:8080",//배포스웨거,
            "http://localhost:5173",   // 프론트 dev
            "http://localhost:8080"//스웨거
    );

    private boolean isTrusted(String url) {
        if (url.matches(PROD_PATTERN)) {
            return true;
        }
        return TRUSTED_ORIGINS.contains(url);
    }
    private boolean hasCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return false;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return true;
        }
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        //제일 먼저, 쓰는 path만 검사해서 걸러내기
        if (!(req.getRequestURI().startsWith("/api/auth/reissue")
                ||req.getRequestURI().startsWith("/api/auth/logout"))) return true;

        final String method  = req.getMethod();
        // 프리플라이트/안전메서드 스킵
        if ("OPTIONS".equalsIgnoreCase(method) || "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            return true;
        }

        // RT 쿠키가 있을 때만 CSRF 검사. (JSESSIONID, signed cookie 무시)
        return !hasCookie(req, TokenType.REFRESH.toString());
    }
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        // Origin/Referer 검증
        String origin = req.getHeader("Origin");
        String referer = req.getHeader("Referer");

        boolean originOk = false;
        if (origin != null) {
            originOk = isTrusted(origin);
        }
        boolean refererOk = false;
        if (referer != null) {
            try {
                URI r = URI.create(referer);
                String schemeHost = r.getScheme() + "://" + r.getHost() + (r.getPort() > 0 ? ":" + r.getPort() : "");
                refererOk = isTrusted(schemeHost);
            } catch (IllegalArgumentException ignored) {}//싹 다 아래 예외 로직으로 넘김
        }

        if (!(originOk || refererOk)) {
            log.warn("CSRF 차단: uri={} method={} origin={} referer={}",
                    req.getRequestURI(), req.getMethod(), origin, referer);

            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("""
                {
                  "isSuccess": false,
                  "code": "CSRF403",
                  "message": "CSRF 검증 실패: 허용되지 않은 Origin, Referer"
                }
                """);
            return;
        }
        chain.doFilter(req, res);
    }
}
