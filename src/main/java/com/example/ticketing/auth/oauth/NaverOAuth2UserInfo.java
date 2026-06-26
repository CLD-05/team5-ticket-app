package com.example.ticketing.auth.oauth;

import java.util.Map;

public class NaverOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    @SuppressWarnings("unchecked")
    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = (Map<String, Object>) attributes.get("response");
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
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