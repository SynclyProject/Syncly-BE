package com.project.syncly.global.oauth.resolver;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.global.anotations.MemberInfo;
import com.project.syncly.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class MemberArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(MemberInfo.class) && parameter.getParameterType().equals(Member.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, org.springframework.web.bind.support.WebDataBinderFactory binderFactory) throws Exception {

        String token = webRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            Long memberId = jwtProvider.getMemberId(token);
            return memberRepository.findById(memberId).orElse(null);
        }
        return null;
    }
}
