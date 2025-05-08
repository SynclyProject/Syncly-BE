package com.project.syncly.domain.member.service.impl;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.exception.MemberErrorCode;
import com.project.syncly.domain.member.exception.MemberException;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.domain.member.service.MemberQueryService;
import com.project.syncly.domain.auth.cache.LoginCacheService;
import com.project.syncly.domain.auth.email.EmailAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryServiceImpl implements MemberQueryService {
    private final MemberRepository memberRepository;
    private final LoginCacheService loginCacheService;
    private final EmailAuthService emailAuthService;

    @Override
    public void sendAuthCode(String email) {
        if(!isEmailExist(email)) {
            emailAuthService.sendAuthCode(email);
        }else{
            log.warn("이미 가입된 이메일입니다: {}", email);
            throw new MemberException(MemberErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    @Override
    public boolean verifyCode(String email, String inputCode) {
        return emailAuthService.verifyCodeAndMarkVerified(email,inputCode);
    }

    @Override
    public boolean isEmailExist(String email) {
        return memberRepository.existsByEmail(email);
    }

    @Override
    public Member getMemberById(Long memberId) {
        Member member = loginCacheService.getCachedMember(memberId);
        if(member == null) {
            member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
            loginCacheService.cacheMember(member);
        }
        return member;
    }

    @Override
    public Member getMemberByEmail(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND_BY_EMAIL));
        loginCacheService.cacheMember(member);
        return member;
    }


}
