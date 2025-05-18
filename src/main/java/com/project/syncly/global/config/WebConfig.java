package com.project.syncly.global.config;

import com.project.syncly.global.anotations.resolver.MemberArgumentResolver;
import com.project.syncly.global.anotations.resolver.MemberIdArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;


@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final MemberArgumentResolver memberArgumentResolver;
    private final MemberIdArgumentResolver memberIdArgumentResolver;

    //멤버랑 아이디 받아올때
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(memberArgumentResolver);
        resolvers.add(memberIdArgumentResolver);
    }

}