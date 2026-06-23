SET NAMES utf8mb4;
-- 1. 사용자 테이블 생성
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 공연 테이블 생성
CREATE TABLE IF NOT EXISTS shows (
    show_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    venue VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 좌석 테이블 생성
CREATE TABLE IF NOT EXISTS seats (
    seat_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    show_id BIGINT NOT NULL,
    seat_number VARCHAR(20) NOT NULL,
    price INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    FOREIGN KEY (show_id) REFERENCES shows(show_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_seats_show_status ON seats (show_id, status);

-- 3-1. 좌석 등급 테이블
CREATE TABLE IF NOT EXISTS seat_grades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    show_id BIGINT,
    grade_name VARCHAR(255),
    price INT,
    total_seats INT,
    remaining_seats INT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_seat_grades_show ON seat_grades (show_id);

-- 4. 예매 테이블 생성
CREATE TABLE IF NOT EXISTS bookings (
    booking_id VARCHAR(36) PRIMARY KEY,
    seat_id BIGINT NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    booked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seat_id) REFERENCES seats(seat_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT uq_bookings_seat UNIQUE (seat_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_bookings_user_id ON bookings (user_id);

-- ==========================================
-- 5. 시드 데이터
-- ==========================================

-- 테스트 유저 (비밀번호: password)
INSERT INTO users (user_id, email, password, name)
VALUES ('7f66a33c-31a8-4c12-9c16-950c40cf12a1', 'user@example.com', '$2a$10$dXJ3SWoGD7Y759vA1OBMse7Fi7W7QKBjY7B6M62aT81q5Zc8M15oG', '테스트유저')
ON DUPLICATE KEY UPDATE name=name;

-- 18개 다채로운 더미 공연 삽입 (DummyDataInitializer와 100% 싱크 매칭)
INSERT INTO shows (show_id, title, venue) VALUES
(1, '임영웅 콘서트 IM HERO', '서울월드컵경기장'),
(2, '싸이 흠뻑쇼 SUMMERSWAG', '잠실종합운동장'),
(3, '아이유 월드투어 HEREH', '서울월드컵경기장'),
(4, '뉴진스 팬미팅 Bunnies Camp', '도쿄돔'),
(5, '방탄소년단 BTS 2026 Reunion', '주경기장'),
(6, '에스파 단독 콘서트 SYNK', '고척스카이돔'),
(7, '뮤지컬 시카고 CHICAGO', '디큐브아트센터'),
(8, '지킬앤하이드 Jekyll & Hyde', '샤롯데씨어터'),
(9, '레미제라블 Les Miserables', '블루스퀘어 신한카드홀'),
(10, '오페라의 유령 PHANTOM', '드림씨어터'),
(11, '2026 워터밤 서울 WATERBOMB', '특설무대'),
(12, '서울재즈페스티벌 SEOUL JAZZ', '올림픽공원 잔디마당'),
(13, '악뮤 10주년 콘서트 10VE', '경희대 평화의전당'),
(14, '데이식스 DAY6 CONCERT', '고척스카이돔'),
(15, '아이브 IVE SHOW WHAT I HAVE', '잠실실내체육관'),
(16, '성시경 축가 콘서트', '연세대 노천극장'),
(17, '조용필 55주년 콘서트', '서울월드컵경기장'),
(18, '뮤지컬 엘리자벳 ELISABETH', '예술의전당 오페라극장')
ON DUPLICATE KEY UPDATE title=title, venue=venue;

-- 좌석 등급 삽입 (각 공연당 VIP 120석, R 120석, S 120석 = 총 360석)
INSERT INTO seat_grades (id, show_id, grade_name, price, total_seats, remaining_seats) VALUES
-- 1. 임영웅
(1, 1, 'VIP', 165000, 120, 120), (2, 1, 'R', 132000, 120, 120), (3, 1, 'S', 99000, 120, 120),
-- 2. 싸이
(4, 2, 'VIP', 165000, 120, 120), (5, 2, 'R', 132000, 120, 120), (6, 2, 'S', 99000, 120, 120),
-- 3. 아이유
(7, 3, 'VIP', 165000, 120, 120), (8, 3, 'R', 132000, 120, 120), (9, 3, 'S', 99000, 120, 120),
-- 4. 뉴진스
(10, 4, 'VIP', 165000, 120, 120), (11, 4, 'R', 132000, 120, 120), (12, 4, 'S', 99000, 120, 120),
-- 5. 방탄소년단
(13, 5, 'VIP', 165000, 120, 120), (14, 5, 'R', 132000, 120, 120), (15, 5, 'S', 99000, 120, 120),
-- 6. 에스파
(16, 6, 'VIP', 165000, 120, 120), (17, 6, 'R', 132000, 120, 120), (18, 6, 'S', 99000, 120, 120),
-- 7. 뮤지컬 시카고
(19, 7, 'VIP', 165000, 120, 120), (20, 7, 'R', 132000, 120, 120), (21, 7, 'S', 99000, 120, 120),
-- 8. 지킬앤하이드
(22, 8, 'VIP', 165000, 120, 120), (23, 8, 'R', 132000, 120, 120), (24, 8, 'S', 99000, 120, 120),
-- 9. 레미제라블
(25, 9, 'VIP', 165000, 120, 120), (26, 9, 'R', 132000, 120, 120), (27, 9, 'S', 99000, 120, 120),
-- 10. 오페라의 유령
(28, 10, 'VIP', 165000, 120, 120), (29, 10, 'R', 132000, 120, 120), (30, 10, 'S', 99000, 120, 120),
-- 11. 워터밤
(31, 11, 'VIP', 165000, 120, 120), (32, 11, 'R', 132000, 120, 120), (33, 11, 'S', 99000, 120, 120),
-- 12. 서울재즈페스티벌
(34, 12, 'VIP', 165000, 120, 120), (35, 12, 'R', 132000, 120, 120), (36, 12, 'S', 99000, 120, 120),
-- 13. 악뮤
(37, 13, 'VIP', 165000, 120, 120), (38, 13, 'R', 132000, 120, 120), (39, 13, 'S', 99000, 120, 120),
-- 14. 데이식스
(40, 14, 'VIP', 165000, 120, 120), (41, 14, 'R', 132000, 120, 120), (42, 14, 'S', 99000, 120, 120),
-- 15. 아이브
(43, 15, 'VIP', 165000, 120, 120), (44, 15, 'R', 132000, 120, 120), (45, 15, 'S', 99000, 120, 120),
-- 16. 성시경
(46, 16, 'VIP', 165000, 120, 120), (47, 16, 'R', 132000, 120, 120), (48, 16, 'S', 99000, 120, 120),
-- 17. 조용필
(49, 17, 'VIP', 165000, 120, 120), (50, 17, 'R', 132000, 120, 120), (51, 17, 'S', 99000, 120, 120),
-- 18. 뮤지컬 엘리자벳
(52, 18, 'VIP', 165000, 120, 120), (53, 18, 'R', 132000, 120, 120), (54, 18, 'S', 99000, 120, 120)
ON DUPLICATE KEY UPDATE grade_name=grade_name, price=price;

-- 18개 공연에 대해 총 6,480개 좌석 벌크 인서트 프로시저 정의 및 실행 (공연당 VIP/R/S 각 A, B, C 구역 40석씩 = 총 360석)
DELIMITER $$
CREATE PROCEDURE PopulateSeats()
BEGIN
    DECLARE show_idx INT DEFAULT 1;
    DECLARE seat_idx INT DEFAULT 1;
    
    WHILE show_idx <= 18 DO
        -- ====================
        -- 1. VIP (V-A, V-B, V-C) 각 40석씩
        -- ====================
        SET seat_idx = 1;
        WHILE seat_idx <= 40 DO
            INSERT INTO seats (show_id, seat_number, price, status)
            VALUES (show_idx, CONCAT('V-A-', seat_idx), 165000, 'AVAILABLE');
            SET seat_idx = seat_idx + 1;
        END WHILE;

        SET seat_idx = 1;
        WHILE seat_idx <= 40 DO
            INSERT INTO seats (show_id, seat_number, price, status)
            VALUES (show_idx, CONCAT('V-B-', seat_idx), 165000, 'AVAILABLE');
            SET seat_idx = seat_idx + 1;
        END WHILE;

        SET seat_idx = 1;
        WHILE seat_idx <= 40 DO
            INSERT INTO seats (show_id, seat_number, price, status)
            VALUES (show_idx, CONCAT('V-C-', seat_idx), 165000, 'AVAILABLE');
            SET seat_idx = seat_idx + 1;
        END WHILE;
        
        -- ====================
        -- 2. R (R-A, R-B, R-C) 각 40석씩
        -- ====================
        SET seat_idx = 1;
        WHILE seat_idx <= 40 DO
            INSERT INTO seats (show_id, seat_number, price, status)
            VALUES (show_idx, CONCAT('R-A-', seat_idx), 132000, 'AVAILABLE');
            SET seat_idx = seat_idx + 1;
        END WHILE;

        SET seat_idx = 1;
        WHILE seat_idx <= 40 DO
            INSERT INTO seats (show_id, seat_number, price, status)
            VALUES (show_idx, CONCAT('R-B-', seat_idx), 132000, 'AVAILABLE');
            SET seat_idx = seat_idx + 1;
        END WHILE;

        SET seat_idx = 1;
        WHILE seat_idx <= 40 DO
            INSERT INTO seats (show_id, seat_number, price, status)
            VALUES (show_idx, CONCAT('R-C-', seat_idx), 132000, 'AVAILABLE');
            SET seat_idx = seat_idx + 1;
        END WHILE;
        
        -- ====================
        -- 3. S (S-A, S-B, S-C) 각 40석씩
        -- ====================
        SET seat_idx = 1;
        WHILE seat_idx <= 40 DO
            INSERT INTO seats (show_id, seat_number, price, status)
            VALUES (show_idx, CONCAT('S-A-', seat_idx), 99000, 'AVAILABLE');
            SET seat_idx = seat_idx + 1;
        END WHILE;

        SET seat_idx = 1;
        WHILE seat_idx <= 40 DO
            INSERT INTO seats (show_id, seat_number, price, status)
            VALUES (show_idx, CONCAT('S-B-', seat_idx), 99000, 'AVAILABLE');
            SET seat_idx = seat_idx + 1;
        END WHILE;

        SET seat_idx = 1;
        WHILE seat_idx <= 40 DO
            INSERT INTO seats (show_id, seat_number, price, status)
            VALUES (show_idx, CONCAT('S-C-', seat_idx), 99000, 'AVAILABLE');
            SET seat_idx = seat_idx + 1;
        END WHILE;
        
        SET show_idx = show_idx + 1;
    END WHILE;
END$$
DELIMITER ;

CALL PopulateSeats();
DROP PROCEDURE PopulateSeats;
