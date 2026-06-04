package com.example.ticketing.show.controller;

import com.example.ticketing.show.entity.Show;
import com.example.ticketing.show.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @GetMapping
    public ResponseEntity<List<Show>> getShows() {
        return ResponseEntity.ok(showService.findAll());
    }

    @GetMapping("/{showId}")
    public ResponseEntity<Show> getShow(@PathVariable Long showId) {
        return ResponseEntity.ok(showService.findById(showId));
    }
}
