package com.project.syncly.domain.member.converter;

import com.project.syncly.domain.member.dto.response.MemberResponseDTO;
import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.entity.SocialLoginProvider;

public class MemberConverter {
    public static Member toSocialMember(String email, String name, SocialLoginProvider provider){
        return Member.builder()
                .email(email)
                .name(name)
                .socialLoginProvider(provider)
                .build();
    }

    public static Member toLocalMember(String email, String encodedPassword, String name) {
        return Member.builder()
                .email(email)
                .password(encodedPassword)
                .name(name)
                .socialLoginProvider(SocialLoginProvider.LOCAL)
                .build();
    }
    public static MemberResponseDTO.MemberInfo toMemberInfo(Member member) {
        return MemberResponseDTO.MemberInfo.builder()
                .name(member.getName())
                .email(member.getEmail())
                .profileImageObjectKey(member.getProfileImage())
                .build();
    }

}
