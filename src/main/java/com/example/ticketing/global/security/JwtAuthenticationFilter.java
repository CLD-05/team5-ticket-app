package com.example.ticketing.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * JWT 검사가 필요 없는 경로는 필터 자체를 타지 않도록 제외
     * 특히 OAuth2 로그인 시작/콜백 경로는 JWT 토큰 없이 접근해야 함
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.equals("/")
                || path.equals("/login")
                || path.equals("/signup")
                || path.equals("/mypage")
                || path.equals("/oauth2/success")

                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")

                // OAuth2 로그인 시작 경로
                || path.startsWith("/oauth2/")

                // OAuth2 콜백 경로
                || path.startsWith("/login/oauth2/")

                // 일반 로그인/회원가입 API
                || path.startsWith("/api/v1/auth/")

                // Swagger
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs/")
                || path.startsWith("/swagger-resources/")
                || path.startsWith("/webjars/")

                // Actuator
                || path.startsWith("/actuator/")

                // 공개 공연 조회 API
                || path.startsWith("/api/v1/shows/")
                || path.startsWith("/api/v1/performances/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String token = resolveToken(request);

        if (token != null) {
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    String userId = jwtTokenProvider.getUserIdFromToken(token);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                request.setAttribute("exception", com.example.ticketing.global.exception.ErrorCode.TOKEN_EXPIRED);
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
                request.setAttribute("exception", com.example.ticketing.global.exception.ErrorCode.TOKEN_INVALID);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}