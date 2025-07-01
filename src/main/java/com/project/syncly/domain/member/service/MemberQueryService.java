package com.project.syncly.domain.member.service;

import com.project.syncly.domain.member.dto.response.MemberResponseDTO;
import com.project.syncly.domain.member.entity.Member;

public interface MemberQueryService {
    public void sendAuthCode(String email);
    public boolean verifyCode(String email, String inputCode);

    public Member getMemberByIdWithRedis(Long memberId);
    public boolean isEmailExist(String email);
    public Member getMemberByEmail(String email);
    MemberResponseDTO.MemberInfo getMyInfo(Member member);
}
