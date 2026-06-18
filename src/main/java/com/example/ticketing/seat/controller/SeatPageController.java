package com.example.ticketing.seat.controller;

import com.example.ticketing.global.exception.BusinessException;
import com.example.ticketing.global.security.SecurityUtils;
import com.example.ticketing.queue.service.QueueService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class SeatPageController {

    private final QueueService queueService;
    
    @GetMapping("/seat")
    public String seatMainPage() {
        return "seat"; 
    }
    
    @GetMapping("/seat/index.html")
    public String seatPage() {
        return "redirect:/shows";
    }

    @GetMapping("/shows/{showId}/seat")
    public String seatPage(@PathVariable Long showId, HttpServletRequest request, Model model) {
        try {
            // Seat page itself is guarded so users cannot skip the queue by opening the page URL directly.
            queueService.validateQueueToken(resolveQueueToken(request), showId, SecurityUtils.getCurrentUserId());
        } catch (BusinessException e) {
            return "redirect:/shows/" + showId + "/queue";
        }

        model.addAttribute("performanceId", showId);
        return "seat";
    }

    private String resolveQueueToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if ("queueToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
