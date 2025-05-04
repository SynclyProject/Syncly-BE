package com.project.syncly.global.config;

import com.project.syncly.global.jwt.*;
import com.project.syncly.global.jwt.service.TokenService;
import com.project.syncly.global.oauth.handler.OAuth2AuthenticationFailureHandler;
import com.project.syncly.global.oauth.handler.OAuth2AuthenticationSuccessHandler;
import com.project.syncly.global.oauth.service.CustomOAuth2UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtProvider jwtProvider;
    private final PrincipalDetailsService principalDetailsService;
    private final TokenService tokenService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final ObjectMapper objectMapper;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oauth2FailureHandler;

    // 허용할 URL을 배열의 형태로 관리
    private final String[] allowedUrls = {
            "/",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/api/auth/login",
            "/api/auth/signup",
            "/api/auth/social/**",
            "/login/oauth2/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtFilter jwtFilter = new JwtFilter(jwtProvider, principalDetailsService, tokenService);

        http
                .csrf(AbstractHttpConfigurer::disable)//jwt는 stateless 로 관리하기 때문에 csrf공격 방어 안해도 됨
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(httpBasic -> httpBasic.disable())//헤더에 비번 담아서 보내는 basic인증방식 jwt에서는 비활성화
                .sessionManagement(session -> session//세션 비활성화(무상태)
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())//bean에서 corsConfigurationSource 찾아와서 알아서 등록
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(allowedUrls).permitAll()
                        // 이외의 요청에 대해서는 인증이 필요하도록 설정
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler)
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        //프론트와 백엔드가 다른 도메인에 있으면 cors 설정 해줘야함

        return http.build();
    }



    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

}
