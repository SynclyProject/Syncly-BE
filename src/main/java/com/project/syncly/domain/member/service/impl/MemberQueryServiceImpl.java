package com.project.syncly.domain.member.service.impl;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.exception.MemberErrorCode;
import com.project.syncly.domain.member.exception.MemberException;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.domain.member.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryServiceImpl implements MemberQueryService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean isEmailExist(String email) {
        return memberRepository.existsByEmail(email);
    }

    @Override
    public Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND_BY_EMAIL));
    }


}
