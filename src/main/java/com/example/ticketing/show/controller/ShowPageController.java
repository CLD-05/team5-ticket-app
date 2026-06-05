package com.example.ticketing.show.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/shows")
public class ShowPageController {

    @GetMapping
    public String showList() {
        return "index";
    }
}