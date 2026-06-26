package com.example.ticketing.auth.oauth;

import com.example.ticketing.global.security.JwtTokenProvider;
import com.example.ticketing.user.entity.User;
import com.example.ticketing.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler
        implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        String email = extractEmail(oauth2User);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        String accessToken = jwtTokenProvider.createToken(user.getUserId(), user.getEmail());

        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(60 * 60); // 1시간
        accessTokenCookie.setHttpOnly(false); // 프론트 JS에서도 읽게 하려면 false
        accessTokenCookie.setSecure(false);   // 로컬 http 테스트는 false

        Cookie userIdCookie = new Cookie("userId", user.getUserId());
        userIdCookie.setPath("/");
        userIdCookie.setMaxAge(60 * 60);
        userIdCookie.setHttpOnly(false);
        userIdCookie.setSecure(false);

        String encodedName = URLEncoder.encode(user.getName(), StandardCharsets.UTF_8);

        Cookie userNameCookie = new Cookie("userName", encodedName);
        userNameCookie.setPath("/");
        userNameCookie.setMaxAge(60 * 60);
        userNameCookie.setHttpOnly(false);
        userNameCookie.setSecure(false);

        response.addCookie(accessTokenCookie);
        response.addCookie(userIdCookie);
        response.addCookie(userNameCookie);

        response.sendRedirect("/");
    }

    private String extractEmail(OAuth2User oauth2User) {
        Object email = oauth2User.getAttribute("email");

        if (email != null) {
            return String.valueOf(email);
        }

        Object response = oauth2User.getAttribute("response");

        if (response instanceof Map<?, ?> responseMap) {
            Object naverEmail = responseMap.get("email");

            if (naverEmail != null) {
                return String.valueOf(naverEmail);
            }
        }

        throw new IllegalArgumentException("OAuth2 이메일 정보를 찾을 수 없습니다.");
    }
}