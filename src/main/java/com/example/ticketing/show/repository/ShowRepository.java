package com.example.ticketing.show.repository;

import com.example.ticketing.show.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    List<Show> findByTitleContainingIgnoreCase(String keyword);

    // 임시 : Show에 category 필드 없음 -> 전체 반환 대체 (수정 및 논의 필요)
   default List<Show> findByCategory(String category) {
		return findAll();
}
}
