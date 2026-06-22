package com.example.ticketing.show.service;

import com.example.ticketing.show.dto.SeatMapResponseDto;
import com.example.ticketing.show.dto.ShowDetailResponseDto;
import com.example.ticketing.show.dto.ShowListResponseDto;
import com.example.ticketing.show.entity.SeatGrade;
import com.example.ticketing.show.entity.Show;
import com.example.ticketing.show.repository.SeatGradeRepository;
import com.example.ticketing.show.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;
    private final SeatGradeRepository seatGradeRepository;
    private final StringRedisTemplate redisTemplate;

    public List<ShowListResponseDto> getShows(String keyword) {

        List<Show> shows;

        if (keyword != null && !keyword.isBlank()) {
            shows = showRepository.findByTitleContainingIgnoreCaseOrVenueContainingIgnoreCase(keyword, keyword);
        } else {
            shows = showRepository.findAll();
        }

        return shows.stream()
                .map(ShowListResponseDto::from)
                .toList();
    }

    public ShowDetailResponseDto getShowDetail(Long showId) {
        try {
            redisTemplate.opsForZSet().incrementScore("popular:shows", String.valueOf(showId), 1.0);
        } catch (Exception e) {
            // Ignore Redis connection issues in case of fallback
        }

        Show show = showRepository.findById(showId)
                .orElseThrow(() ->
                        new IllegalArgumentException("공연을 찾을 수 없습니다."));

        return ShowDetailResponseDto.from(show);
    }

    public SeatMapResponseDto getSeatMap(Long showId) {

        Show show = showRepository.findById(showId)
                .orElseThrow(() ->
                        new IllegalArgumentException("공연을 찾을 수 없습니다."));

        List<SeatGrade> seatGrades = seatGradeRepository.findByShowId(showId);

        return SeatMapResponseDto.builder()
                .showId(show.getShowId())
                .venueName(show.getVenue())
                .seatGrades(
                        seatGrades.stream()
                                .map(seat ->
                                        SeatMapResponseDto.SeatGradeDto.builder()
                                                .gradeName(seat.getGradeName())
                                                .price(seat.getPrice())
                                                .totalSeats(seat.getTotalSeats())
                                                .remainingSeats(seat.getRemainingSeats())
                                                .build())
                                .toList()
                )
                .build();
    }

    public List<ShowListResponseDto> getPopularShows() {
        Set<String> popularShowIds = redisTemplate.opsForZSet().reverseRange("popular:shows", 0, 9);
        if (popularShowIds == null || popularShowIds.isEmpty()) {
            return List.of();
        }
        List<Long> idsInOrder = popularShowIds.stream()
                .map(Long::valueOf)
                .toList();
        List<Show> shows = showRepository.findAllById(idsInOrder);
        List<Show> sortedShows = new ArrayList<>(shows);
        sortedShows.sort(java.util.Comparator.comparingInt(show -> idsInOrder.indexOf(show.getShowId())));
        return sortedShows.stream()
                .map(ShowListResponseDto::from)
                .toList();
    }
}