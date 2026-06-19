package com.example.ticketing.admin.service;

import com.example.ticketing.admin.dto.BulkCreateSeatsRequest;
import com.example.ticketing.admin.dto.CreateShowRequest;
import com.example.ticketing.global.exception.NotFoundException;
import com.example.ticketing.queue.service.QueueService;
import com.example.ticketing.show.entity.SeatGrade;
import com.example.ticketing.show.entity.Show;
import com.example.ticketing.show.repository.SeatGradeRepository;
import com.example.ticketing.show.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final ShowRepository showRepository;
    private final SeatGradeRepository seatGradeRepository;
    private final QueueService queueService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public Show createShow(CreateShowRequest request) {
        Show show = Show.builder()
                .title(request.getTitle())
                .venue(request.getVenue())
                .build();
        Show savedShow = showRepository.save(show);
        log.info("Successfully created show: id={}, title={}", savedShow.getShowId(), savedShow.getTitle());
        return savedShow;
    }

    @Transactional
    public void bulkCreateSeats(Long showId, BulkCreateSeatsRequest request) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new NotFoundException("공연을 찾을 수 없습니다. ID: " + showId));

        if (request.getGrades() == null || request.getGrades().isEmpty()) {
            return;
        }

        for (BulkCreateSeatsRequest.GradeItem item : request.getGrades()) {
            String gradeName = item.getGradeName();
            int price = item.getPrice();
            int totalSeats = item.getTotalSeats();
            if (totalSeats <= 0) {
                continue; // Skip if no seats specified
            }
            String prefix = item.getPrefix();
            if (prefix == null || prefix.isBlank()) {
                prefix = gradeName;
            }

            // 1. Create or Update SeatGrade
            Optional<SeatGrade> existingGradeOpt = seatGradeRepository.findFirstByShowIdAndPrice(showId, price);
            if (existingGradeOpt.isPresent()) {
                SeatGrade grade = existingGradeOpt.get();
                grade.setTotalSeats(grade.getTotalSeats() + totalSeats);
                grade.setRemainingSeats(grade.getRemainingSeats() + totalSeats);
                seatGradeRepository.save(grade);
            } else {
                SeatGrade grade = SeatGrade.builder()
                        .showId(showId)
                        .gradeName(gradeName)
                        .price(price)
                        .totalSeats(totalSeats)
                        .remainingSeats(totalSeats)
                        .build();
                seatGradeRepository.save(grade);
            }

            // 2. Generate seat numbers and bulk insert using JdbcTemplate
            List<String> seatNumbers = new ArrayList<>(totalSeats);
            for (int i = 1; i <= totalSeats; i++) {
                seatNumbers.add(prefix + "-" + i);
            }

            String sql = "INSERT INTO seats (show_id, seat_number, price, status, version) VALUES (?, ?, ?, ?, 0)";
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setLong(1, showId);
                    ps.setString(2, seatNumbers.get(i));
                    ps.setInt(3, price);
                    ps.setString(4, "AVAILABLE");
                }

                @Override
                public int getBatchSize() {
                    return seatNumbers.size();
                }
            });

            log.info("Successfully bulk-created {} seats of grade {} (price {}) for showId: {}", totalSeats, gradeName, price, showId);
        }
    }

    @Transactional
    public void deleteShow(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new NotFoundException("공연을 찾을 수 없습니다. ID: " + showId));

        // 1. Delete associated bookings
        jdbcTemplate.update("DELETE FROM bookings WHERE seat_id IN (SELECT seat_id FROM seats WHERE show_id = ?)", showId);

        // 2. Delete associated seats
        jdbcTemplate.update("DELETE FROM seats WHERE show_id = ?", showId);

        // 3. Delete associated seat_grades
        jdbcTemplate.update("DELETE FROM seat_grades WHERE show_id = ?", showId);

        // 4. Delete the show
        showRepository.delete(show);

        // 5. Clean up Redis queues and tokens
        queueService.clearQueue(showId);

        log.info("Successfully deleted show: id={}, title={} and all associated seats/grades/bookings/queues", showId, show.getTitle());
    }

    public void resetQueue(Long showId) {
        if (showId != null) {
            queueService.clearQueue(showId);
        } else {
            queueService.clearAllQueues();
        }
    }

    @Transactional(readOnly = true)
    public com.example.ticketing.admin.dto.ShowStatsResponse getShowStats(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new NotFoundException("공연을 찾을 수 없습니다. ID: " + showId));

        List<SeatGrade> grades = seatGradeRepository.findByShowId(showId);

        String sql = "SELECT price, status, COUNT(*) as cnt FROM seats WHERE show_id = ? GROUP BY price, status";
        List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(sql, showId);

        java.util.Map<Integer, java.util.Map<String, Integer>> statusMapByPrice = new java.util.HashMap<>();
        for (java.util.Map<String, Object> row : rows) {
            int price = ((Number) row.get("price")).intValue();
            String status = (String) row.get("status");
            int count = ((Number) row.get("cnt")).intValue();

            statusMapByPrice.putIfAbsent(price, new java.util.HashMap<>());
            statusMapByPrice.get(price).put(status, count);
        }

        List<com.example.ticketing.admin.dto.ShowStatsResponse.GradeStats> statsList = new java.util.ArrayList<>();
        long totalRevenue = 0;

        for (SeatGrade grade : grades) {
            int price = grade.getPrice();
            java.util.Map<String, Integer> counts = statusMapByPrice.getOrDefault(price, java.util.Collections.emptyMap());

            int available = counts.getOrDefault("AVAILABLE", 0);
            int hold = counts.getOrDefault("HOLD", 0);
            int sold = counts.getOrDefault("SOLD", 0);
            int total = available + hold + sold;

            if (total == 0) {
                total = grade.getTotalSeats() != null ? grade.getTotalSeats() : 0;
            }

            double soldRate = total > 0 ? (double) sold * 100.0 / total : 0.0;
            soldRate = Math.round(soldRate * 100.0) / 100.0;

            totalRevenue += (long) sold * price;

            statsList.add(com.example.ticketing.admin.dto.ShowStatsResponse.GradeStats.builder()
                    .gradeName(grade.getGradeName())
                    .price(price)
                    .total(total)
                    .available(available)
                    .hold(hold)
                    .sold(sold)
                    .soldRate(soldRate)
                    .build());
        }

        return com.example.ticketing.admin.dto.ShowStatsResponse.builder()
                .showId(show.getShowId())
                .title(show.getTitle())
                .venue(show.getVenue())
                .totalRevenue(totalRevenue)
                .stats(statsList)
                .build();
    }
}
