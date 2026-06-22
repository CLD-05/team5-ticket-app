package com.example.ticketing.show.repository;

import com.example.ticketing.show.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {
 
    List<Show> findByTitleContainingIgnoreCaseOrVenueContainingIgnoreCase(String title, String venue);
}