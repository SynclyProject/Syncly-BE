package com.project.syncly.domain.member.service;

import com.project.syncly.domain.member.dto.request.MemberRequestDTO;
import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.entity.SocialLoginProvider;

public interface MemberCommandService {
    public Member registerMember(MemberRequestDTO.SignUp dto);
    public Member findOrCreateSocialMember(String email, String name, SocialLoginProvider provider);

}
