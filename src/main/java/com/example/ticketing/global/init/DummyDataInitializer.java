package com.example.ticketing.global.init;

import com.example.ticketing.admin.dto.BulkCreateSeatsRequest;
import com.example.ticketing.admin.service.AdminService;
import com.example.ticketing.show.entity.Show;
import com.example.ticketing.show.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@Profile({"local", "docker", "db-init"})
@RequiredArgsConstructor
public class DummyDataInitializer implements CommandLineRunner {

    private final ShowRepository showRepository;
    private final AdminService adminService;
    private final Environment environment;
    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting database cleanup and dummy data seeding process...");

        // 18개 다채로운 더미 공연 정의
        List<DummyShowInfo> dummyShows = Arrays.asList(
                new DummyShowInfo("임영웅 콘서트 IM HERO", "서울월드컵경기장", "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=800", "2026-08-10 19:00:00", "2026-06-15 20:00:00", "2026-08-09 23:59:59"),
                new DummyShowInfo("뮤지컬 시카고 CHICAGO", "디큐브아트센터", "https://images.unsplash.com/photo-1460723237483-7a6dc9d0b212?w=800", "2026-08-30 19:30:00", "2026-07-16 14:00:00", "2026-08-29 23:59:59"),
                new DummyShowInfo("악뮤 10주년 콘서트 10VE", "경희대 평화의전당", "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=800", "2026-06-12 18:00:00", "2026-05-12 20:00:00", "2026-06-11 23:59:59"),
                new DummyShowInfo("싸이 흠뻑쇼 SUMMERSWAG", "잠실종합운동장", "https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?w=800", "2026-08-15 18:30:00", "2026-06-16 20:00:00", "2026-08-14 23:59:59"),
                new DummyShowInfo("지킬앤하이드 Jekyll & Hyde", "샤롯데씨어터", "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=800", "2026-09-15 19:30:00", "2026-07-17 14:00:00", "2026-09-14 23:59:59"),
                new DummyShowInfo("데이식스 DAY6 CONCERT", "고척스카이돔", "https://images.unsplash.com/photo-1524368535928-5b5e00ddc76b?w=800", "2026-06-30 18:00:00", "2026-05-30 20:00:00", "2026-06-29 23:59:59"),
                new DummyShowInfo("아이유 월드투어 HEREH", "서울월드컵경기장", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800", "2026-09-20 18:00:00", "2026-06-17 20:00:00", "2026-09-19 23:59:59"),
                new DummyShowInfo("레미제라블 Les Miserables", "블루스퀘어 신한카드홀", "https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?w=800", "2026-10-01 19:30:00", "2026-07-20 14:00:00", "2026-09-30 23:59:59"),
                new DummyShowInfo("아이브 IVE SHOW WHAT I HAVE", "잠실실내체육관", "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=800", "2026-06-05 18:00:00", "2026-05-05 20:00:00", "2026-06-04 23:59:59"),
                new DummyShowInfo("뉴진스 팬미팅 Bunnies Camp", "도쿄돔", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=800", "2026-08-20 17:00:00", "2026-06-18 20:00:00", "2026-08-19 23:59:59"),
                new DummyShowInfo("오페라의 유령 PHANTOM", "드림씨어터", "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=800", "2026-10-15 19:30:00", "2026-07-25 14:00:00", "2026-10-14 23:59:59"),
                new DummyShowInfo("성시경 축가 콘서트", "연세대 노천극장", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=800", "2026-05-16 19:00:00", "2026-04-16 20:00:00", "2026-05-15 23:59:59"),
                new DummyShowInfo("방탄소년단 BTS 2026 Reunion", "주경기장", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800", "2026-08-25 19:00:00", "2026-06-19 20:00:00", "2026-08-24 23:59:59"),
                new DummyShowInfo("2026 워터밤 서울 WATERBOMB", "특설무대", "https://images.unsplash.com/photo-1482440308425-276ad0f28b19?w=800", "2026-08-25 13:00:00", "2026-07-22 13:00:00", "2026-08-24 23:59:59"),
                new DummyShowInfo("조용필 55주년 콘서트", "서울월드컵경기장", "https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?w=800", "2026-06-21 18:00:00", "2026-05-21 20:00:00", "2026-06-20 23:59:59"),
                new DummyShowInfo("에스파 단독 콘서트 SYNK", "고척스카이돔", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=800", "2026-08-15 18:00:00", "2026-06-20 20:00:00", "2026-08-14 23:59:59"),
                new DummyShowInfo("서울재즈페스티벌 SEOUL JAZZ", "올림픽공원 잔디마당", "https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=800", "2026-08-23 12:00:00", "2026-07-24 12:00:00", "2026-08-22 23:59:59"),
                new DummyShowInfo("뮤지컬 엘리자벳 ELISABETH", "예술의전당 오페라극장", "https://images.unsplash.com/photo-1503095391758-11200cf53674?w=800", "2026-06-08 14:00:00", "2026-05-08 14:00:00", "2026-06-07 23:59:59")
        );

        // 1. 기존 지저분한 임시 테스트 데이터 클렌징 (Test, Sihyun, Trot, 게임, ??? 등) + 기존 더미 데이터의 좌석구조 갱신을 위해 기존 더미도 삭제
        List<Show> allShows = showRepository.findAll();
        for (Show s : allShows) {
            String title = s.getTitle();
            boolean isOldDummy = dummyShows.stream().anyMatch(d -> d.title.equalsIgnoreCase(title));
            if (title == null || 
                isOldDummy ||
                title.contains("Test") || 
                title.contains("Sihyun") || 
                title.contains("Trot") || 
                title.contains("?") || 
                title.contains("어드민") || 
                title.contains("게임")) {
                
                try {
                    adminService.deleteShow(s.getShowId());
                    log.info("🗑️ Cleaned up old/dirty show: ID={}, Title='{}'", s.getShowId(), title);
                } catch (Exception e) {
                    log.warn("Failed to delete dirty show: ID={}, Title='{}'", s.getShowId(), title, e);
                }
            }
        }

        // 2. 남은 공연 개수 확인
        long count = showRepository.count();
        log.info("Current cleaned show count: {}", count);

        // DB에 있는 최신 공연 리스트 다시 로드
        List<Show> existingShows = showRepository.findAll();
        java.time.ZoneId zoneId = java.time.ZoneId.of("Asia/Seoul");

        for (DummyShowInfo info : dummyShows) {
            // 중복 생성 방지
            boolean exists = existingShows.stream()
                    .anyMatch(s -> s.getTitle().equalsIgnoreCase(info.title));
            if (exists) {
                continue;
            }
            
            java.time.LocalDateTime performanceAt = java.time.LocalDateTime.parse(info.performanceAtStr.replace(" ", "T"));
            java.time.LocalDateTime bookingOpenAt = java.time.LocalDateTime.parse(info.bookingOpenAtStr.replace(" ", "T"));
            java.time.LocalDateTime bookingCloseAt = java.time.LocalDateTime.parse(info.bookingCloseAtStr.replace(" ", "T"));
            
            // 2.1. 공연 등록
            Show show = Show.builder()
                    .title(info.title)
                    .venue(info.venue)
                    .imageUrl(info.imageUrl)
                    .performanceAt(performanceAt)
                    .bookingOpenAt(bookingOpenAt)
                    .bookingCloseAt(bookingCloseAt)
                    .build();
            Show savedShow = showRepository.save(show);
            Long showId = savedShow.getShowId();
  
            // 2.2. 좌석 및 좌석등급 등록 (VIP/R/S 각 A, B, C 구역 40석씩 = 등급당 120석, 총 360석)
            BulkCreateSeatsRequest seatRequest = BulkCreateSeatsRequest.builder()
                    .grades(Arrays.asList(
                        // VIP
                        BulkCreateSeatsRequest.GradeItem.builder().gradeName("VIP").price(165000).totalSeats(40).prefix("V-A").build(),
                        BulkCreateSeatsRequest.GradeItem.builder().gradeName("VIP").price(165000).totalSeats(40).prefix("V-B").build(),
                        BulkCreateSeatsRequest.GradeItem.builder().gradeName("VIP").price(165000).totalSeats(40).prefix("V-C").build(),
                        // R
                        BulkCreateSeatsRequest.GradeItem.builder().gradeName("R").price(132000).totalSeats(40).prefix("R-A").build(),
                        BulkCreateSeatsRequest.GradeItem.builder().gradeName("R").price(132000).totalSeats(40).prefix("R-B").build(),
                        BulkCreateSeatsRequest.GradeItem.builder().gradeName("R").price(132000).totalSeats(40).prefix("R-C").build(),
                        // S
                        BulkCreateSeatsRequest.GradeItem.builder().gradeName("S").price(99000).totalSeats(40).prefix("S-A").build(),
                        BulkCreateSeatsRequest.GradeItem.builder().gradeName("S").price(99000).totalSeats(40).prefix("S-B").build(),
                        BulkCreateSeatsRequest.GradeItem.builder().gradeName("S").price(99000).totalSeats(40).prefix("S-C").build()
                    ))
                    .build();
            
            try {
                adminService.bulkCreateSeats(showId, seatRequest);
                log.info("• Show ID: {} [{}] seats generated (360 seats, A/B/C zones).", showId, info.title);
            } catch (Exception e) {
                log.error("Failed to generate seats for showId: {}", showId, e);
            }
        }

        log.info("Dummy data initialization completed successfully!");
        shutdownAfterDbInit();
    }

    private void shutdownAfterDbInit() {
        if (!Arrays.asList(environment.getActiveProfiles()).contains("db-init")) {
            return;
        }

        log.info("db-init profile detected. Shutting down application after dummy data initialization.");
        int exitCode = SpringApplication.exit(applicationContext);
        System.exit(exitCode);
    }

    private static class DummyShowInfo {
        String title;
        String venue;
        String imageUrl;
        String performanceAtStr;
        String bookingOpenAtStr;
        String bookingCloseAtStr;

        DummyShowInfo(String title, String venue, String imageUrl, String performanceAtStr, String bookingOpenAtStr, String bookingCloseAtStr) {
            this.title = title;
            this.venue = venue;
            this.imageUrl = imageUrl;
            this.performanceAtStr = performanceAtStr;
            this.bookingOpenAtStr = bookingOpenAtStr;
            this.bookingCloseAtStr = bookingCloseAtStr;
        }
    }
}
