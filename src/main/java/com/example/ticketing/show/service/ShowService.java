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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;
    private final SeatGradeRepository seatGradeRepository;
    private final StringRedisTemplate redisTemplate;
    private static final String WAITING_QUEUE_KEY_PREFIX = "queue:waiting:";
    private static final String ACTIVE_QUEUE_KEY_PREFIX = "queue:active:";
    private static final int NORMAL_CONGESTION_THRESHOLD = 500;
    private static final int VERY_BUSY_CONGESTION_THRESHOLD = 2000;

    @Transactional(readOnly = true)
    public List<ShowListResponseDto> getShows(String keyword) {

        List<Show> shows;

        if (keyword != null && !keyword.isBlank()) {
            shows = showRepository.findByTitleContainingIgnoreCaseOrVenueContainingIgnoreCase(keyword, keyword);
        } else {
            shows = showRepository.findAll();
        }

        return shows.stream()
                .map(this::toShowListResponse)
                .toList();
    }
    @Transactional(readOnly = true)
    public ShowDetailResponseDto getShowDetail(Long showId) {
        try {
            redisTemplate.opsForZSet().incrementScore("popular:shows", String.valueOf(showId), 1.0);
        } catch (Exception e) {
            // Ignore Redis connection issues in case of fallback
        }

        Show show = showRepository.findById(showId)
                .orElseThrow(() ->
                        new IllegalArgumentException("공연을 찾을 수 없습니다."));

        String openTimeStr = redisTemplate.opsForValue().get("show:" + showId + ":booking_open_at");
        String closeTimeStr = redisTemplate.opsForValue().get("show:" + showId + ":booking_close_at");
        String perfTimeStr = redisTemplate.opsForValue().get("show:" + showId + ":performance_at");

        Long bookingOpenAt = null;
        Long bookingCloseAt = null;
        Long performanceAt = null;

        try {
            if (openTimeStr != null) bookingOpenAt = Long.parseLong(openTimeStr);
            if (closeTimeStr != null) bookingCloseAt = Long.parseLong(closeTimeStr);
            if (perfTimeStr != null) performanceAt = Long.parseLong(perfTimeStr);
        } catch (NumberFormatException e) {
            // Ignore parse errors
        }

        return ShowDetailResponseDto.from(show, bookingOpenAt, bookingCloseAt, performanceAt);
    }
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
                .map(this::toShowListResponse)
                .toList();
    }

    private ShowListResponseDto toShowListResponse(Show show) {
        CongestionInfo congestionInfo = congestionInfo(show.getShowId());
        return ShowListResponseDto.from(show, congestionInfo.status(), congestionInfo.label());
    }

    private CongestionInfo congestionInfo(Long showId) {
        BookingWindow bookingWindow = bookingWindow(showId);
        long now = Instant.now().getEpochSecond();

        if (bookingWindow.openAt() != null && now < bookingWindow.openAt()) {
            return new CongestionInfo("UPCOMING", "오픈 예정");
        }
        if (bookingWindow.closeAt() != null && now > bookingWindow.closeAt()) {
            return new CongestionInfo("CLOSED", "예매 마감");
        }

        long congestionScore = queueCount(WAITING_QUEUE_KEY_PREFIX + showId) + queueCount(ACTIVE_QUEUE_KEY_PREFIX + showId);
        if (congestionScore >= VERY_BUSY_CONGESTION_THRESHOLD) {
            return new CongestionInfo("VERY_BUSY", "매우 혼잡");
        }
        if (congestionScore >= NORMAL_CONGESTION_THRESHOLD) {
            return new CongestionInfo("NORMAL", "보통");
        }
        return new CongestionInfo("SMOOTH", "원활");
    }

    private BookingWindow bookingWindow(Long showId) {
        Long openAt = readEpochSecond("show:" + showId + ":booking_open_at");
        Long closeAt = readEpochSecond("show:" + showId + ":booking_close_at");
        return new BookingWindow(openAt, closeAt);
    }

    private Long readEpochSecond(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value == null ? null : Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }

    private long queueCount(String key) {
        try {
            Long count = redisTemplate.opsForZSet().zCard(key);
            return count == null ? 0 : count;
        } catch (Exception e) {
            return 0;
        }
    }

    private record BookingWindow(Long openAt, Long closeAt) {
    }

    private record CongestionInfo(String status, String label) {
    }
}
