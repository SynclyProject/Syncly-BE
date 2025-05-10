package com.project.syncly.global.anotations.resolver;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.service.MemberQueryService;
import com.project.syncly.global.anotations.MemberInfo;
import com.project.syncly.global.jwt.JwtProvider;
import com.project.syncly.global.jwt.enums.TokenType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.bind.support.WebDataBinderFactory;

@Component
@RequiredArgsConstructor
public class MemberArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtProvider jwtProvider;
    private final MemberQueryService memberQueryService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(MemberInfo.class) && parameter.getParameterType().equals(Member.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        String token = webRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);

            TokenType tokenType = jwtProvider.getTokenType(token);
            Long memberId = jwtProvider.getMemberIdWithBlacklistCheck(token, tokenType);
            return memberQueryService.getMemberById(memberId);
        }
        return null;
    }
}
