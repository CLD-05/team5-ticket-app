package com.example.ticketing.queue.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/shows/{showId}/queue")
public class QueuePageController {

    @GetMapping
    public String queuePage(@PathVariable Long showId, Model model) {
        model.addAttribute("showId", showId);
        return "queue";
    }
}