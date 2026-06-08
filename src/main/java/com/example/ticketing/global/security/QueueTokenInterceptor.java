package com.example.ticketing.global.security;

import com.example.ticketing.global.exception.BusinessException;
import com.example.ticketing.global.exception.ErrorCode;
import com.example.ticketing.queue.service.QueueService;
import com.example.ticketing.seat.repository.SeatRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class QueueTokenInterceptor implements HandlerInterceptor {

    private final QueueService queueService;
    private final SeatRepository seatRepository;

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

        String token = resolveQueueToken(request);
        Long showId = resolveShowId(request);
        queueService.validateQueueToken(token, showId, userId);

        return true;
    }

    private String resolveQueueToken(HttpServletRequest request) {
        String token = request.getHeader("X-Queue-Token");
        if (StringUtils.hasText(token)) {
            return token;
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("queueToken".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    private Long resolveShowId(HttpServletRequest request) {
        String path = request.getRequestURI();

        Long showIdFromPath = extractPathId(path, "/api/v1/shows/");
        if (showIdFromPath != null) {
            return showIdFromPath;
        }

        Long performanceIdFromPath = extractPathId(path, "/api/v1/performances/");
        if (performanceIdFromPath != null) {
            return performanceIdFromPath;
        }

        Long seatIdFromPath = extractPathId(path, "/api/v1/seats/");
        if (seatIdFromPath != null) {
            return seatRepository.findShowIdBySeatId(seatIdFromPath)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        }

        String seatId = request.getParameter("seatId");
        if (StringUtils.hasText(seatId)) {
            return seatRepository.findShowIdBySeatId(Long.parseLong(seatId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        }

        throw new BusinessException(ErrorCode.INVALID_QUEUE_TOKEN);
    }

    private Long extractPathId(String path, String prefix) {
        if (!path.startsWith(prefix)) {
            return null;
        }

        String rest = path.substring(prefix.length());
        int slashIndex = rest.indexOf('/');
        String id = slashIndex >= 0 ? rest.substring(0, slashIndex) : rest;

        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
