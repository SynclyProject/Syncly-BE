package com.project.syncly.domain.auth.oauth.dto;

import lombok.Builder;

@Builder
public record AccessTokenResponse(
        String accessToken,
        long   accessExpiresInSec
) {}
