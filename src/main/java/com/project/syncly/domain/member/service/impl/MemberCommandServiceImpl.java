package com.project.syncly.domain.member.service.impl;

import com.project.syncly.domain.auth.service.AuthService;
import com.project.syncly.domain.member.converter.MemberConverter;
import com.project.syncly.domain.member.dto.request.MemberRequestDTO;
import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.entity.SocialLoginProvider;
import com.project.syncly.domain.member.exception.MemberErrorCode;
import com.project.syncly.domain.member.exception.MemberException;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.domain.member.service.MemberCommandService;
import com.project.syncly.domain.auth.cache.LoginCacheService;
import com.project.syncly.domain.auth.email.EmailAuthService;
import com.project.syncly.domain.s3.dto.S3RequestDTO;
import com.project.syncly.domain.s3.exception.S3ErrorCode;
import com.project.syncly.domain.s3.exception.S3Exception;
import com.project.syncly.domain.s3.util.S3Util;
import com.project.syncly.domain.workspace.service.WorkspaceService;
import com.project.syncly.domain.workspaceMember.entity.WorkspaceMember;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import com.project.syncly.global.redis.core.RedisStorage;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberCommandServiceImpl implements MemberCommandService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginCacheService loginCacheService;
    private final EmailAuthService emailAuthService;
    private final AuthService authService;
    private final S3Util s3Util;
    private final RedisStorage redisStorage;
    private final WorkspaceService workspaceService;
    private final WorkspaceMemberRepository workspaceMemberRepository;




    @Override
    public void registerMember(MemberRequestDTO.SignUp dto) {
        if (!emailAuthService.isVerified(dto.email())) {
            throw new MemberException(MemberErrorCode.EMAIL_NOT_VERIFIED);
        }
        String encodedPassword = passwordEncoder.encode(dto.password());

        Member member = MemberConverter.toLocalMember(dto.email(), encodedPassword, dto.name());

        memberRepository.save(member);
        workspaceService.createPersonalWorkspace(member.getId());
        emailAuthService.clearVerified(dto.email());
    }


    @Override
    public Member findOrCreateSocialMember(String email, String name, SocialLoginProvider provider) {
        Member member = memberRepository.findByEmail(email).orElse(null);
        if (member == null) {
            member  = memberRepository.save(MemberConverter.toSocialMember(email, name, provider));
            workspaceService.createPersonalWorkspace(member .getId());
        }
        loginCacheService.cacheMember(member);
        return member;
    }

    @Override
    public void updateName(MemberRequestDTO.UpdateName updateName,Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        member.updateName(updateName.newName());

        List<WorkspaceMember> workspaceMembers = workspaceMemberRepository.findAllByMember(member);
        workspaceMembers.forEach(workspaceMember -> {
            workspaceMember.updateName(updateName.newName());
        });

        loginCacheService.cacheMember(member);
    }

    @Override
    public void updateProfileImage(Long memberId, S3RequestDTO.UpdateFile request) {
        String redisKey = RedisKeyPrefix.S3_AUTH_OBJECT_KEY.get(
                memberId.toString() + ':' + request.fileName() + ':' + request.objectKey());

        S3RequestDTO.ProfileImageUploadPreSignedUrl saved = redisStorage.getValueAsString(
                redisKey, S3RequestDTO.ProfileImageUploadPreSignedUrl.class
        );

        if (saved == null) {
            throw new S3Exception(S3ErrorCode.OBJECT_KEY_NOT_FOUND);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (member.getProfileImage() != null) {
            s3Util.delete(member.getProfileImage());
        }
        List<WorkspaceMember> workspaceMembers = workspaceMemberRepository.findAllByMember(member);

        member.updateProfileImage(request.objectKey());
        workspaceMembers.forEach(workspaceMember ->
                workspaceMember.updateProfileImage(request.objectKey())
        );
        redisStorage.delete(redisKey);
        loginCacheService.cacheMember(member);
    }

    @Override
    public void deleteProfileImage(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (member.getProfileImage() == null) {
            throw new MemberException(MemberErrorCode.PROFILE_IMAGE_NOT_FOUND);
        }

        s3Util.delete(member.getProfileImage());
        member.updateProfileImage(null);
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
    public void updatePasswordWithEmail(MemberRequestDTO.UpdatePasswordWithEmail updatePassword) {
        Member member = memberRepository.findByEmail(updatePassword.email())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (member.getSocialLoginProvider() != SocialLoginProvider.LOCAL) {
            throw new MemberException(MemberErrorCode.SOCIAL_MEMBER_CANNOT_USE_THIS_FEATURE);
        }
        // 이메일 인증여부 확인
        boolean isVerified = emailAuthService.isVerifiedBeforeChangePassword(member.getEmail());

        if (!isVerified) {
            throw new MemberException(MemberErrorCode.EMAIL_NOT_VERIFIED);
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
        authService.logout(request, response);
        loginCacheService.removeMemberCache(member.getId());
    }
}
