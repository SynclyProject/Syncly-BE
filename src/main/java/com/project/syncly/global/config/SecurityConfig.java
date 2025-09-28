package com.project.syncly.global.config;

import com.project.syncly.global.reissueFilter.CsrfFilter;
import com.project.syncly.global.jwt.*;
import com.project.syncly.domain.auth.oauth.handler.OAuth2AuthenticationFailureHandler;
import com.project.syncly.domain.auth.oauth.handler.OAuth2AuthenticationSuccessHandler;
import com.project.syncly.domain.auth.oauth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtProvider jwtProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oauth2FailureHandler;

    // 허용할 URL을 배열의 형태로 관리
    private final String[] allowedUrls = {
            //인증 헤더를 사용하지 않음(로그아웃은 csrf만)
            "/api/auth/login",
            "/api/auth/reissue",
            "/api/auth/logout",
            //소셜
            "/api/auth/social/**",
            "/api/login/oauth2/**",
            //회원가입
            "/api/member/email/send",
            "/api/member/email/verify",
            "/api/member/register",
            //livekit
            "/api/livekit/webhook",
            "/ws-stomp",
            "/ws-stomp/**",
            "/api/workspaces/notifications",
            "/api/workspaces/notifications/**",
            //비밀번호
            "/api/member/password/**"
    };

    @Bean
    @Order(0)
    SecurityFilterChain docsChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(
                        "/",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**"
                )
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(1)
    SecurityFilterChain oauth2Chain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/oauth2/**",//클라이언트 초기 접속 api (GET /oauth2/authorization/{registrationId})
                        "/api/login/oauth2/**")//구글에서 우리서버 redirect url (GET /login/oauth2/code/{registrationId})
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // OAuth2는 state 보관 등으로 세션이 필요할 수 있으므로 IF_REQUIRED
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(a -> a.baseUri("/api/oauth2/authorization"))  // 시작 URL 변경
                        .redirectionEndpoint(r -> r.baseUri("/api/login/oauth2/code/*"))     // 콜백 URL 변경
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler)
                );
        return http.build();
    }


    @Bean
    @Order(2)
    public SecurityFilterChain apiChain(HttpSecurity http,
                                        CsrfFilter csrfFilter) throws Exception {
        JwtFilter jwtFilter = new JwtFilter(jwtProvider);

        http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)//커스텀 필터 따로 사용(기본은 세션기반)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(httpBasic -> httpBasic.disable())//헤더에 비번 담아서 보내는 basic인증방식 jwt에서는 비활성화
                .sessionManagement(session -> session//세션 비활성화(무상태)
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())//bean에서 corsConfigurationSource 찾아와서 알아서 등록
                .authorizeHttpRequests(a -> a
                        // 프리플라이트는 항상 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(allowedUrls).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                // 필터 순서: CORS → (조건부) CSRF → JWT
                .addFilterAfter(csrfFilter, org.springframework.web.filter.CorsFilter.class)
                .addFilterAfter(jwtFilter, CsrfFilter.class);

        return http.build();
    }

//    폴백 체인: 위 체인에 안 걸린 모든 경로 차단(의도치 않은 노출 방지)
    @Bean
    @Order(99)
    SecurityFilterChain fallbackChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a -> a.anyRequest().denyAll());
        return http.build();
    }

    //Websocket handshake시 filter chain을 지나지 않고 무시하도록 설정(해당 설정이 없으면 403에러 발생)
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/ws-stomp");
    }

}