package com.project.syncly.domain.member.service;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.exception.MemberErrorCode;
import com.project.syncly.domain.member.exception.MemberException;
import org.springframework.transaction.annotation.Transactional;

public interface MemberQueryService {
    public void sendAuthCode(String email);
    public boolean verifyCode(String email, String inputCode);

    public Member getMemberById(Long memberId);
    public boolean isEmailExist(String email);
    public Member getMemberByEmail(String email);
}
