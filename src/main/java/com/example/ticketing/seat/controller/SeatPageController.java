package com.example.ticketing.seat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SeatPageController {

    @GetMapping("/seat/index.html")
    public String seatPage() {
        return "seat";
    }
}
