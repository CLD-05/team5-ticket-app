package com.example.ticketing.booking.entity;

import com.example.ticketing.seat.entity.Seat;
import com.example.ticketing.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "bookings",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_bookings_seat", columnNames = "seat_id")
    },
    indexes = {
        @Index(name = "idx_bookings_user_id", columnList = "user_id")
    }
)
@Getter
@NoArgsConstructor
public class Booking {
    @Id
    @Column(name = "booking_id", length = 36)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "booked_at", nullable = false)
    private LocalDateTime bookedAt;

    public Booking(Seat seat, User user) {
        this.id = UUID.randomUUID().toString();
        this.seat = seat;
        this.user = user;
        this.bookedAt = LocalDateTime.now();
    }
}
