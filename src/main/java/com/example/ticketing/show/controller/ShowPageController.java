package com.example.ticketing.show.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/shows")
public class ShowPageController {

    @GetMapping
    public String showList() {
        return "index";
    }

    @GetMapping("/{showId}")
    public String showDetail(@PathVariable Long showId, Model model) {
        model.addAttribute("showId", showId);
        return "show-detail";
    }
}