package com.example.ticketing.show.repository;

import com.example.ticketing.show.entity.SeatGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatGradeRepository extends JpaRepository<SeatGrade, Long> {

    List<SeatGrade> findByShowId(Long showId);
}