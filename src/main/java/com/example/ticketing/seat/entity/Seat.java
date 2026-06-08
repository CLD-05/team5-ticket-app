package com.example.ticketing.seat.entity;

import com.example.ticketing.show.entity.Show;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "seats",
    indexes = {
        @Index(name = "idx_seats_show_status", columnList = "show_id,status")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @Column(name = "seat_number", nullable = false, length = 20)
    private String seatNumber;

    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status = SeatStatus.AVAILABLE;

    public Seat(Show show, String seatNumber, int price) {
        this.show = show;
        this.seatNumber = seatNumber;
        this.price = price;
        this.status = SeatStatus.AVAILABLE;
    }

    public void hold() {
        this.status = SeatStatus.HOLD;
    }

    public void sold() {
        this.status = SeatStatus.SOLD;
    }

    public void available() {
        this.status = SeatStatus.AVAILABLE;
    }

    public boolean isAvailable() {
        return this.status == SeatStatus.AVAILABLE;
    }
}