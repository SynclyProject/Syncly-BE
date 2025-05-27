package com.project.syncly.domain.member.dto.response;

import lombok.Builder;

public class MemberResponseDTO {
    @Builder
    public record MemberTokenDTO (
        String accessToken,
        String refreshToken
    ){ }
    @Builder
    public record MemberInfo(
            String name,
            String email,
            String profileImageObjectKey
    ){ }

}