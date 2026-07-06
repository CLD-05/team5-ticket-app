package com.example.ticketing.global.config;

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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // AntPathRequestMatcher.antMatcher(...) 로 명시 → MvcRequestMatcher 우회.
                // 기존 문자열 requestMatchers("...") 는 Spring이 MvcRequestMatcher 로 해석하여
                // 매 요청 PathPatternMatchableHandlerMapping.match → ConcurrentHashMap.computeIfAbsent
                // 버킷 락을 타므로, 5만 동시요청에서 스레드가 직렬화(busy 659)됨.
                // AntPathRequestMatcher 는 순수 문자열 매칭이라 HandlerMapping 참조/락이 없음.
                .requestMatchers(
                        AntPathRequestMatcher.antMatcher("/"),
                        AntPathRequestMatcher.antMatcher("/login"),
                        AntPathRequestMatcher.antMatcher("/signup"),
                        AntPathRequestMatcher.antMatcher("/mypage"),
                        AntPathRequestMatcher.antMatcher("/css/**"),
                        AntPathRequestMatcher.antMatcher("/js/**")
                ).permitAll()
                .requestMatchers(
                        AntPathRequestMatcher.antMatcher("/api/v1/auth/**"),
                        AntPathRequestMatcher.antMatcher("/actuator/**")
                ).permitAll()
                .requestMatchers(
                        AntPathRequestMatcher.antMatcher("/swagger-ui.html"),
                        AntPathRequestMatcher.antMatcher("/swagger-ui/**"),
                        AntPathRequestMatcher.antMatcher("/v3/api-docs/**"),
                        AntPathRequestMatcher.antMatcher("/swagger-resources/**"),
                        AntPathRequestMatcher.antMatcher("/webjars/**")
                ).permitAll()
                .requestMatchers(
                        AntPathRequestMatcher.antMatcher("/api/v1/admin/**"),
                        AntPathRequestMatcher.antMatcher("/admin")
                ).permitAll()
                // 부하 핵심 경로 — 여기가 5만에서 락을 탔던 지점
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/shows/*/queue/**")).authenticated()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/shows/**")).permitAll()
                .requestMatchers(
                        AntPathRequestMatcher.antMatcher("/shows"),
                        AntPathRequestMatcher.antMatcher("/shows/**"),
                        AntPathRequestMatcher.antMatcher("/seat"),
                        AntPathRequestMatcher.antMatcher("/seat/**")
                ).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/performances/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/queue/**")).authenticated()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/seats/**")).authenticated()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/bookings/**")).authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(handler -> handler.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}