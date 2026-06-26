package com.example.ticketing.auth.oauth;

import java.util.Map;

public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("sub"));
    }

    @Override
    public String getEmail() {
        return String.valueOf(attributes.get("email"));
    }

    @Override
    public String getName() {
        return String.valueOf(attributes.get("name"));
    }
}