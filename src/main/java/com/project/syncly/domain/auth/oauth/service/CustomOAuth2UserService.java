package com.project.syncly.domain.auth.oauth.service;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.entity.SocialLoginProvider;
import com.project.syncly.domain.member.service.MemberCommandService;
import com.project.syncly.domain.auth.oauth.attribute.OAuthAttributes;
import com.project.syncly.global.jwt.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberCommandService memberCommandService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // userRequest에서 accessToken을 꺼내서 userInfoUri로 요청보내서 사용자 정보 받아옴
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId(); // kakao
        SocialLoginProvider socialLoginProvider = SocialLoginProvider.from(provider);

        OAuthAttributes attributes = OAuthAttributes.of(socialLoginProvider, oAuth2User.getAttributes());

        String email = provider + "_" + attributes.getEmail();
        String name = attributes.getName();

        Member member = memberCommandService.findOrCreateSocialMember(email, name, socialLoginProvider); // 또는 GOOGLE


        return new PrincipalDetails(member, oAuth2User.getAttributes());
    }


}
