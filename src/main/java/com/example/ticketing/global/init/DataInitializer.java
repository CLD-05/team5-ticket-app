package com.example.ticketing.global.init;

import com.example.ticketing.seat.entity.Seat;
import com.example.ticketing.seat.repository.SeatRepository;
import com.example.ticketing.show.entity.Show;
import com.example.ticketing.show.repository.ShowRepository;
import com.example.ticketing.user.entity.User;
import com.example.ticketing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            log.info("Seeding default user...");
            User user = User.builder()
                    .userId("test-user-id-1")
                    .email("test@example.com")
                    .password(passwordEncoder.encode("password"))
                    .name("이제훈")
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(user);
            log.info("Seeding default user completed: test@example.com / password");
        }

        if (showRepository.count() == 0) {
            log.info("Seeding default shows and seats...");
            Show show1 = new Show("임영웅 콘서트 [IM HERO]", "서울월드컵경기장");
            Show show2 = new Show("뮤지컬 [지킬앤하이드]", "샤롯데씨어터");

            showRepository.save(show1);
            showRepository.save(show2);

            // Seed seats for Show 1 (50 seats)
            for (int i = 1; i <= 25; i++) {
                seatRepository.save(new Seat(show1, "A-" + i, 150000));
            }
            for (int i = 1; i <= 25; i++) {
                seatRepository.save(new Seat(show1, "B-" + i, 120000));
            }

            // Seed seats for Show 2 (50 seats)
            for (int i = 1; i <= 25; i++) {
                seatRepository.save(new Seat(show2, "VIP-" + i, 180000));
            }
            for (int i = 1; i <= 25; i++) {
                seatRepository.save(new Seat(show2, "R-" + i, 140000));
            }
            log.info("Seeding default shows and seats completed.");
        }
    }
}
