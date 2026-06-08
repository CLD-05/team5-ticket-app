package com.example.ticketing.global.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {
    /**
     * 현재 인증된 사용자의 UUID(userId)를 반환합니다.
     * 다른 팀원(대기열, 좌석, 예약 담당자)들이 이 메서드를 사용하여 요청자를 식별합니다.
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return (String) authentication.getPrincipal();
    }
}
