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
    private static final Set<String> TRUSTED_ORIGINS = Set.of(
            "https://syncly-io.com",//프론트엔드 도메인(https://app.syncly-io.com 이런식일수도)
            "http://52.79.102.15:8080/",//배포스웨거,
            "http://localhost:5173",   // 프론트 dev
            "http://localhost:8080"//스웨거

    );

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

        final String m = req.getMethod();
        // 프리플라이트/안전메서드 스킵
        if ("OPTIONS".equalsIgnoreCase(m)) return true;
        if ("GET".equalsIgnoreCase(m) || "HEAD".equalsIgnoreCase(m)) return true;


        // RT 쿠키가 있을 때만 CSRF 검사. (JSESSIONID, signed cookie 무시)
        return !hasCookie(req, TokenType.REFRESH.toString());
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
