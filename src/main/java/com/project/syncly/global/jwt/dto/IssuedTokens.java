package com.project.syncly.global.jwt.dto;

// 새 토큰 세트 반환용
public record IssuedTokens(
        String accessToken,
        String refreshToken,
        long   accessExpiresInSec,
        long   refreshExpiresInSec
) {}