package com.project.syncly.domain.member.entity;

import com.project.syncly.global.oauth.exception.AuthErrorCode;
import com.project.syncly.global.oauth.exception.AuthException;

public enum SocialLoginProvider {
    LOCAL,
    GOOGLE;

    public static SocialLoginProvider from(String provider) {
        return switch (provider.toLowerCase()) {
            case "google" -> GOOGLE;
            case "local" -> LOCAL;
            default -> throw new AuthException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        };
    }
}
