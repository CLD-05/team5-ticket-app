package com.example.ticketing.show.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seat_grades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long showId;

    private String gradeName;

    private Integer price;

    private Integer totalSeats;

    private Integer remainingSeats;
}