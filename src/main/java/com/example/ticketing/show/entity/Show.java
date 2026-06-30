package com.example.ticketing.show.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "shows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "show_id")
    private Long showId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String venue;

    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(name = "performance_at", nullable = false)
    private LocalDateTime performanceAt;

    @Column(name = "booking_open_at", nullable = false)
    private LocalDateTime bookingOpenAt;

    @Column(name = "booking_close_at", nullable = false)
    private LocalDateTime bookingCloseAt;
    
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}