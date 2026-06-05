package com.example.ticketing.show.controller;

import com.example.ticketing.show.dto.*;
import com.example.ticketing.show.service.ShowService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @GetMapping
    public List<ShowListResponseDto> getShows(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category
    ) {
        return showService.getShows(keyword, category);
    }

    @GetMapping("/{showId}")
    public ShowDetailResponseDto getShowDetail(
            @PathVariable Long showId
    ) {
        return showService.getShowDetail(showId);
    }

    @GetMapping("/{showId}/seat-map")
    public SeatMapResponseDto getSeatMap(
            @PathVariable Long showId
    ) {
        return showService.getSeatMap(showId);
    }
}