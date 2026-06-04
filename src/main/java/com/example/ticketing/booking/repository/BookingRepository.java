package com.example.ticketing.booking.repository;

import com.example.ticketing.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, String> {
}
