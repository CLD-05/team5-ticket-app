package com.example.ticketing.global.config;

import com.example.ticketing.auth.oauth.CustomOAuth2UserService;
import com.example.ticketing.auth.oauth.OAuth2SuccessHandler;
import com.example.ticketing.global.security.JwtAuthenticationEntryPoint;
import com.example.ticketing.global.security.JwtAuthenticationFilter;
import com.example.ticketing.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)

            // OAuth2 로그인 과정에서는 state 저장을 위해 세션이 필요할 수 있음
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/login",
                    "/signup",
                    "/mypage",
                    "/css/**",
                    "/js/**",
                    "/images/**",

                    // OAuth2 로그인 시작/콜백 경로
                    "/oauth2/**",
                    "/login/oauth2/**",

                    // OAuth2 성공 후 리다이렉트 받을 경로
                    "/oauth2/success"
                ).permitAll()

                .requestMatchers("/api/v1/auth/**", "/actuator/**").permitAll()

                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()

                .requestMatchers("/api/v1/admin/**", "/admin").permitAll()

                .requestMatchers("/api/v1/shows/*/queue/**").authenticated()
                .requestMatchers("/api/v1/shows/**").permitAll()
                .requestMatchers("/", "/shows", "/shows/**", "/seat", "/seat/**").permitAll()
                .requestMatchers("/api/v1/performances/**").permitAll()

                .requestMatchers("/api/v1/queue/**").authenticated()
                .requestMatchers("/api/v1/seats/**").authenticated()
                .requestMatchers("/api/v1/bookings/**").authenticated()

                .anyRequest().authenticated()
            )

            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2SuccessHandler)
                .failureUrl("/login?error")
            )

            .exceptionHandling(handler -> handler.authenticationEntryPoint(jwtAuthenticationEntryPoint))

            .addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}