package com.project.syncly.global.jwt.dto;

import java.time.Instant;

// Refresh 토큰에서 뽑은 클레임
public record RefreshClaims(
        long    userId,
        String  deviceId,
        String  jti,
        Instant expiresAt
) {}