package com.example.ticketing.show.controller;

import com.example.ticketing.show.dto.SeatMapResponseDto;
import com.example.ticketing.show.dto.ShowDetailResponseDto;
import com.example.ticketing.show.dto.ShowListResponseDto;
import com.example.ticketing.show.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @GetMapping
    public List<ShowListResponseDto> getShows(
            @RequestParam(required = false) String keyword
    ) {
        return showService.getShows(keyword);
    }

    @GetMapping("/{showId}")
    public ShowDetailResponseDto getShowDetail(
            @PathVariable Long showId
    ) {
        return showService.getShowDetail(showId);
    }

    @GetMapping("/{showId}/seats")
    public SeatMapResponseDto getSeatMap(
            @PathVariable Long showId
    ) {
        return showService.getSeatMap(showId);
    }
}