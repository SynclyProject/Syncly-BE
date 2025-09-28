package com.project.syncly.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {//cross-origin 응답을 JS에서 읽을 수 있는지 통제
    private static final List<String> PROD_ORIGINS = List.of(
            "https://syncly-io.com",
            "https://*.syncly-io.com", //도메인 모든 패턴 허용
            "http://localhost:*",
            "http://localhost:3000",
            "http://localhost:5173",// react dev
            "http://127.0.0.1:5500"
    );

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        //쿠키 허용 api
        // 크리덴셜 필요한 경로들
        List<String> CRED_PATHS = List.of(
                "/api/auth/**",
                "/api/livekit/**",
                "/api/s3/view-cookie",
                //oauth2
                "/api/oauth2/**",
                "/api/login/oauth2/**"
        );
        CorsConfiguration cred = new CorsConfiguration();
        cred.setAllowedOriginPatterns(PROD_ORIGINS);
        cred.setAllowCredentials(true);// 쿠키를 포함한 요청 허용
        cred.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        cred.setAllowedHeaders(List.of("*"));
        cred.setMaxAge(3600L);
        for (String path : CRED_PATHS) {
            source.registerCorsConfiguration(path, cred);
        }

        //쿠키 비 허용
        CorsConfiguration nonCred = new CorsConfiguration();
        nonCred.setAllowedOriginPatterns(PROD_ORIGINS);
        nonCred.setAllowCredentials(false);// 쿠키를 포함한 요청 거부
        nonCred.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        nonCred.setAllowedHeaders(List.of("*"));
        nonCred.setMaxAge(3600L);
        source.registerCorsConfiguration("/api/**", nonCred);

        return source;
    }
}
