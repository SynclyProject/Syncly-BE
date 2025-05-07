package com.project.syncly.domain.auth.oauth.attribute;

import com.project.syncly.domain.member.entity.SocialLoginProvider;
import com.project.syncly.domain.auth.oauth.exception.AuthErrorCode;
import com.project.syncly.domain.auth.oauth.exception.AuthException;
import lombok.Getter;

import java.util.Map;

@Getter
public class OAuthAttributes {

    private final String email;
    private final String name;

    public static OAuthAttributes of(SocialLoginProvider SocialLoginProvider, Map<String, Object> attributes) {
        return switch (SocialLoginProvider) {
            case GOOGLE -> ofGoogle(attributes);
            default -> throw new AuthException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        };
    }


    private static OAuthAttributes ofGoogle(Map<String, Object> attributes) {
        return new OAuthAttributes(
                (String) attributes.get("email"),
                (String) attributes.get("name")
        );
    }

    public OAuthAttributes(String email, String name) {
        this.email = email;
        this.name = name;
    }
}
