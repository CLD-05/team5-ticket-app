package com.example.ticketing.auth.oauth;

public interface OAuth2UserInfo {
    String getProviderId();
    String getEmail();
    String getName();
}