package com.example.ticketing.show.service;

import com.example.ticketing.show.repository.SeatGradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeatGradeService {

    private final SeatGradeRepository seatGradeRepository;

    @Transactional
    public void decreaseRemainingSeats(Long seatGradeId) {
        int updated = seatGradeRepository.decreaseRemainingSeats(seatGradeId);

        if (updated == 0) {
            throw new IllegalStateException("잔여 좌석이 없습니다.");
        }
    }

    @Transactional
    public void increaseRemainingSeats(Long seatGradeId) {
        seatGradeRepository.increaseRemainingSeats(seatGradeId);
    }
}