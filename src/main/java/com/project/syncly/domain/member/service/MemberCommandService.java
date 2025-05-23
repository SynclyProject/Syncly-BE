package com.project.syncly.domain.member.service;

import com.project.syncly.domain.member.dto.request.MemberRequestDTO;
import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.entity.SocialLoginProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface MemberCommandService {
    public void registerMember(MemberRequestDTO.SignUp dto);
    public Member findOrCreateSocialMember(String email, String name, SocialLoginProvider provider);
    public void updateName(MemberRequestDTO.UpdateName updateName, Long memberId);
    public void updatePassword(MemberRequestDTO.UpdatePassword updatePassword, Long memberId);
    public void updateProfileImage(Long memberId, S3RequestDTO.UpdateFile request);
    public void deleteProfileImage(Long memberId);
    public void deleteMember(HttpServletRequest request, HttpServletResponse response,
                             Long memberId, MemberRequestDTO.DeleteMember toDelete);
    public void updatePassword(MemberRequestDTO.UpdatePassword updatePassword, Long memberId);
}
