package com.example.ticketing.seat.repository;

import com.example.ticketing.seat.entity.Seat;
import com.example.ticketing.seat.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    
    @Query("SELECT s FROM Seat s WHERE s.show.showId = :showId")
    List<Seat> findByShowId(@Param("showId") Long showId);
    
    @Query("SELECT s FROM Seat s WHERE s.show.showId = :showId AND s.status = :status")
    List<Seat> findByShowIdAndStatus(@Param("showId") Long showId, @Param("status") SeatStatus status);
    
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findByIdWithLock(@Param("id") Long id);
    
    // Queue Token 검증 적용 시 seatId로 showId 조회
    // @Query("SELECT s.show.showId FROM Seat s WHERE s.id = :id")
    // Optional<Long> findShowIdBySeatId(@Param("id") Long id);
}
