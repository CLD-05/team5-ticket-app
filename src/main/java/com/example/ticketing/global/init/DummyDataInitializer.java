package com.example.ticketing.global.init;

import com.example.ticketing.admin.dto.BulkCreateSeatsRequest;
import com.example.ticketing.admin.service.AdminService;
import com.example.ticketing.show.entity.Show;
import com.example.ticketing.show.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DummyDataInitializer implements CommandLineRunner {

    private final ShowRepository showRepository;
    private final AdminService adminService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting database cleanup and dummy data seeding process...");

        // 1. 기존 지저분한 임시 테스트 데이터 클렌징 (Test, Sihyun, Trot, 게임, ??? 등)
        List<Show> allShows = showRepository.findAll();
        for (Show s : allShows) {
            String title = s.getTitle();
            if (title == null || 
                title.contains("Test") || 
                title.contains("Sihyun") || 
                title.contains("Trot") || 
                title.contains("?") || 
                title.contains("어드민") || 
                title.contains("게임")) {
                
                try {
                    adminService.deleteShow(s.getShowId());
                    log.info("🗑️ Cleaned up dirty test show: ID={}, Title='{}'", s.getShowId(), title);
                } catch (Exception e) {
                    log.warn("Failed to delete dirty show: ID={}, Title='{}'", s.getShowId(), title, e);
                }
            }
        }

        // 2. 남은 공연 개수 확인
        long count = showRepository.count();
        log.info("Current cleaned show count: {}", count);

        // 18개 다채로운 더미 공연 정의 (임영웅, 싸이, 아이유 등)
        List<DummyShowInfo> dummyShows = Arrays.asList(
            new DummyShowInfo("임영웅 콘서트 IM HERO", "서울월드컵경기장", "active"),
            new DummyShowInfo("싸이 흠뻑쇼 SUMMERSWAG", "잠실종합운동장", "before"),
            new DummyShowInfo("아이유 월드투어 HEREH", "서울월드컵경기장", "closed"),
            new DummyShowInfo("뉴진스 팬미팅 Bunnies Camp", "도쿄돔", "active"),
            new DummyShowInfo("방탄소년단 BTS 2026 Reunion", "주경기장", "active"),
            new DummyShowInfo("에스파 단독 콘서트 SYNK", "고척스카이돔", "active"),
            new DummyShowInfo("뮤지컬 시카고 CHICAGO", "디큐브아트센터", "active"),
            new DummyShowInfo("지킬앤하이드 Jekyll & Hyde", "샤롯데씨어터", "active"),
            new DummyShowInfo("레미제라블 Les Miserables", "블루스퀘어 신한카드홀", "active"),
            new DummyShowInfo("오페라의 유령 PHANTOM", "드림씨어터", "active"),
            new DummyShowInfo("2026 워터밤 서울 WATERBOMB", "특설무대", "active"),
            new DummyShowInfo("서울재즈페스티벌 SEOUL JAZZ", "올림픽공원 잔디마당", "active"),
            new DummyShowInfo("악뮤 10주년 콘서트 10VE", "경희대 평화의전당", "active"),
            new DummyShowInfo("데이식스 DAY6 CONCERT", "고척스카이돔", "active"),
            new DummyShowInfo("아이브 IVE SHOW WHAT I HAVE", "잠실실내체육관", "active"),
            new DummyShowInfo("성시경 축가 콘서트", "연세대 노천극장", "active"),
            new DummyShowInfo("조용필 55주년 콘서트", "서울월드컵경기장", "active"),
            new DummyShowInfo("뮤지컬 엘리자벳 ELISABETH", "예술의전당 오페라극장", "active")
        );

        long now = System.currentTimeMillis() / 1000;
        long tenYears = 315360000L;
        long fiveYears = 157680000L;

        // DB에 있는 최신 공연 리스트 다시 로드
        List<Show> existingShows = showRepository.findAll();

        for (DummyShowInfo info : dummyShows) {
            // 중복 생성 방지
            boolean exists = existingShows.stream()
                    .anyMatch(s -> s.getTitle().equalsIgnoreCase(info.title));
            if (exists) {
                continue;
            }

            // 2.1. 공연 등록
            Show show = Show.builder()
                    .title(info.title)
                    .venue(info.venue)
                    .build();
            Show savedShow = showRepository.save(show);
            Long showId = savedShow.getShowId();

            // 2.2. Redis에 영구 시뮬레이션 타임스탬프 적재
            Long openAt = null, closeAt = null, perfAt = null;
            if ("before".equals(info.state)) {
                openAt = now + tenYears;
                closeAt = now + tenYears + 3600;
                perfAt = now + tenYears + 7200;
            } else if ("active".equals(info.state)) {
                openAt = now - tenYears;
                closeAt = now + tenYears;
                perfAt = now + tenYears + 3600;
            } else if ("closed".equals(info.state)) {
                openAt = now - tenYears;
                closeAt = now - fiveYears;
                perfAt = now + tenYears;
            }

            if (openAt != null) {
                redisTemplate.opsForValue().set("show:" + showId + ":booking_open_at", String.valueOf(openAt));
            }
            if (closeAt != null) {
                redisTemplate.opsForValue().set("show:" + showId + ":booking_close_at", String.valueOf(closeAt));
            }
            if (perfAt != null) {
                redisTemplate.opsForValue().set("show:" + showId + ":performance_at", String.valueOf(perfAt));
            }

            // 2.3. 좌석 및 좌석등급 등록 (VIP 80, R 80, S 80 = 총 240석)
            BulkCreateSeatsRequest seatRequest = BulkCreateSeatsRequest.builder()
                    .grades(Arrays.asList(
                        BulkCreateSeatsRequest.GradeItem.builder().gradeName("VIP").price(165000).totalSeats(80).prefix("V-A").build(),
                        BulkCreateSeatsRequest.GradeItem.builder().gradeName("R").price(132000).totalSeats(80).prefix("R-A").build(),
                        BulkCreateSeatsRequest.GradeItem.builder().gradeName("S").price(99000).totalSeats(80).prefix("S-A").build()
                    ))
                    .build();
            
            try {
                adminService.bulkCreateSeats(showId, seatRequest);
                log.info("• Show ID: {} [{}] seats generated successfully.", showId, info.title);
            } catch (Exception e) {
                log.error("Failed to generate seats for showId: {}", showId, e);
            }
        }

        log.info("Dummy data initialization completed successfully!");
    }

    private static class DummyShowInfo {
        String title;
        String venue;
        String state;

        DummyShowInfo(String title, String venue, String state) {
            this.title = title;
            this.venue = venue;
            this.state = state;
        }
    }
}
