package com.example.ticketing.auth.oauth;

import com.example.ticketing.user.domain.AuthProvider;

import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(
            AuthProvider provider,
            Map<String, Object> attributes
    ) {
        return switch (provider) {
            case GOOGLE -> new GoogleOAuth2UserInfo(attributes);
            case NAVER -> new NaverOAuth2UserInfo(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 OAuth2 Provider입니다.");
        };
    }
}