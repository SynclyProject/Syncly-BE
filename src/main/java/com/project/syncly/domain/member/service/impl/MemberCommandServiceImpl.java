package com.project.syncly.domain.member.service.impl;

import com.project.syncly.domain.member.converter.MemberConverter;
import com.project.syncly.domain.member.dto.request.MemberRequestDTO;
import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.entity.SocialLoginProvider;
import com.project.syncly.domain.member.exception.MemberErrorCode;
import com.project.syncly.domain.member.exception.MemberException;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.domain.member.service.MemberCommandService;
import com.project.syncly.domain.member.service.MemberQueryService;
import com.project.syncly.domain.auth.cache.LoginCacheService;
import com.project.syncly.domain.auth.email.EmailAuthService;
import com.project.syncly.global.jwt.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final LoginCacheService loginCacheService;
    private final EmailAuthService emailAuthService;
    private final TokenService tokenService;



    @Override
    public void registerMember(MemberRequestDTO.SignUp dto) {
        if (!emailAuthService.isVerified(dto.email())) {
            throw new MemberException(MemberErrorCode.EMAIL_NOT_VERIFIED);
        }
        String encodedPassword = passwordEncoder.encode(dto.password());

        Member member = MemberConverter.toMember(dto.email(), encodedPassword, dto.name());

        memberRepository.save(member);
        emailAuthService.clearVerified(dto.email());
    }


    @Override
    public Member findOrCreateSocialMember(String email, String name, SocialLoginProvider provider) {

        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(
                        MemberConverter.toSocialMember(email, name, provider)
                ));
        loginCacheService.cacheMember(member);
        return member;
    }

    @Override
    public void updateName(MemberRequestDTO.UpdateName updateName,Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        member.updateName(updateName.newName());
        loginCacheService.cacheMember(member);

    }

    @Override
    public void updatePassword(MemberRequestDTO.UpdatePassword updatePassword, Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (member.getSocialLoginProvider() != SocialLoginProvider.LOCAL) {
            throw new MemberException(MemberErrorCode.SOCIAL_MEMBER_CANNOT_USE_THIS_FEATURE);
        }

        if (!passwordEncoder.matches(updatePassword.currentPassword(), member.getPassword())) {
            throw new MemberException(MemberErrorCode.PASSWORD_NOT_MATCHED);
        }
        String encodedNewPassword = passwordEncoder.encode(updatePassword.newPassword());
        member.updatePassword(encodedNewPassword);

        loginCacheService.cacheMember(member);
    }

    @Override
    public void deleteMember(HttpServletRequest request, HttpServletResponse response,
                             Long memberId, MemberRequestDTO.DeleteMember toDelete) {
        //삭제전엔 안전하게 db에서 조회
        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        if (!passwordEncoder.matches(toDelete.password(), member.getPassword())) {
            throw new MemberException(MemberErrorCode.PASSWORD_NOT_MATCHED);
        }
        member.markAsDeleted(toDelete.leaveReasonType(), toDelete.leaveReason());
        tokenService.logout(request, response);
        loginCacheService.removeMemberCache(member.getId());
    }
}
