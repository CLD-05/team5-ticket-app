package com.example.ticketing.global.security;

import com.example.ticketing.global.exception.BusinessException;
import com.example.ticketing.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class QueueTokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Allow reading own booking list/detail and cancellations without queue token check.
        // Queue token check is only required for checking seats, holding seats, and booking POST requests.
        if (path.startsWith("/api/v1/bookings") && !method.equalsIgnoreCase("POST")) {
            return true;
        }

        String token = request.getHeader("X-Queue-Token");
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        String savedToken = redisTemplate.opsForValue().get("queue:token:" + userId);
        if (savedToken == null) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }

        if (!savedToken.equals(token)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        return true;
    }
}
