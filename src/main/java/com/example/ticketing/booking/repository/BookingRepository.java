package com.example.ticketing.booking.repository;

import com.example.ticketing.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, String> {

    @Query("SELECT b FROM Booking b JOIN FETCH b.seat s JOIN FETCH s.show WHERE b.user.userId = :userId ORDER BY b.bookedAt DESC")
    List<Booking> findByUserIdWithSeatAndShow(@Param("userId") String userId);
}
