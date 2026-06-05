package com.example.ticketing.booking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BookingPageController {

    // P-05 결제 처리 화면
    @GetMapping("/booking/process")
    public String processPage() {
        return "booking-process";   // templates/booking-process.html
    }

    // P-06 완료 화면
    @GetMapping("/booking/complete")
    public String completePage() {
        return "booking-complete";  // templates/booking-complete.html
    }
}
