package com.example.ticketing.auth.oauth;

import com.example.ticketing.user.domain.AuthProvider;
import com.example.ticketing.user.entity.User;
import com.example.ticketing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest
                .getClientRegistration()
                .getRegistrationId();

        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        Map<String, Object> attributes = oauth2User.getAttributes();

        OAuth2UserInfo userInfo =
                OAuth2UserInfoFactory.getOAuth2UserInfo(provider, attributes);

        User user = userRepository.findByEmail(userInfo.getEmail())
                .orElseGet(() -> createUser(userInfo, provider));

        String nameAttributeKey = userRequest
                .getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        return new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                nameAttributeKey
        );
    }

    private User createUser(OAuth2UserInfo userInfo, AuthProvider provider) {
        String name = userInfo.getName();

        if (name == null || name.isBlank()) {
            name = userInfo.getEmail();
        }

        User user = User.builder()
                .email(userInfo.getEmail())
                .password("SOCIAL_LOGIN")
                .name(name)
                .provider(provider)
                .providerId(userInfo.getProviderId())
                .build();

        return userRepository.save(user);
    }
}