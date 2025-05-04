package com.project.syncly.domain.member.service.impl;

import com.project.syncly.domain.member.dto.request.MemberRequestDTO;
import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.entity.SocialLoginProvider;
import com.project.syncly.domain.member.exception.MemberErrorCode;
import com.project.syncly.domain.member.exception.MemberException;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.domain.member.service.MemberCommandService;
import com.project.syncly.domain.member.service.MemberQueryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberCommandServiceImpl implements MemberCommandService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberQueryService memberQueryService;

    @Override
    public Member registerMember(MemberRequestDTO.signUp dto) {
        if (memberQueryService.isEmailExist(dto.email())) {
            throw new MemberException(MemberErrorCode.EMAIL_ALREADY_EXISTS);
        }
        Member member = Member.builder()
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .name(dto.name())
                .build();

        return memberRepository.save(member);
    }


    @Override
    public Member findOrCreateSocialMember(String email, String name, SocialLoginProvider provider) {
        return memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(
                        Member.builder()
                                .email(email)
                                .name(name)
                                .socialLoginProvider(provider)
                                .build()
                ));
    }
}
