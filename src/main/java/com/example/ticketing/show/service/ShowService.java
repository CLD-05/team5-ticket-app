package com.example.ticketing.show.service;

import com.example.ticketing.global.exception.NotFoundException;
import com.example.ticketing.show.entity.Show;
import com.example.ticketing.show.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;

    @Transactional(readOnly = true)
    public List<Show> findAll() {
        return showRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Show findById(Long id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("공연을 찾을 수 없습니다."));
    }
}
