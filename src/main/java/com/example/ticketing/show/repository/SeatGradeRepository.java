package com.example.ticketing.show.repository;

import com.example.ticketing.show.entity.SeatGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;

public interface SeatGradeRepository extends JpaRepository<SeatGrade, Long> {

    List<SeatGrade> findByShowId(Long showId);
    Optional<SeatGrade> findFirstByShowIdAndPrice(Long showId, Integer price); 

    // 잔여석이 남아있을 때만 1 감소 (원자적). 영향 행 수 반환 → 0이면 매진
    @Modifying
    @Query("UPDATE SeatGrade sg SET sg.remainingSeats = sg.remainingSeats - 1 " +
           "WHERE sg.id = :seatGradeId AND sg.remainingSeats > 0")
    int decreaseRemainingSeats(@Param("seatGradeId") Long seatGradeId);

    // 잔여석 1 증가 (취소/롤백용)
    @Modifying
    @Query("UPDATE SeatGrade sg SET sg.remainingSeats = sg.remainingSeats + 1 " +
           "WHERE sg.id = :seatGradeId")
    int increaseRemainingSeats(@Param("seatGradeId") Long seatGradeId);
}
