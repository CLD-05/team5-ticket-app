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

-- 3. 좌석 테이블 생성 (JPA 엔티티의 version, created_at, updated_at 칼럼 추가)
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

-- [성능 최적화 1] 공연 예매창 진입 시 특정 공연의 예매 가능(AVAILABLE) 좌석을 초고속 조회하기 위한 복합 인덱스
CREATE INDEX idx_seats_show_status ON seats (show_id, status);

-- 4. 예매 테이블 생성
CREATE TABLE IF NOT EXISTS bookings (
    booking_id VARCHAR(36) PRIMARY KEY,
    seat_id BIGINT NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    booked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seat_id) REFERENCES seats(seat_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT uq_bookings_seat UNIQUE (seat_id) -- 최종 동시성 방어용 유니크 인덱스
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [성능 최적화 2] 마이페이지에서 로그인한 사용자가 자신의 예매 내역을 조회하는 속도를 극대화하는 인덱스
CREATE INDEX idx_bookings_user_id ON bookings (user_id);

-- ==========================================
-- 5. 테스트용 시드 데이터 삽입
-- ==========================================

-- 테스트 유저 (비밀번호: password)
INSERT INTO users (user_id, email, password, name) 
VALUES ('7f66a33c-31a8-4c12-9c16-950c40cf12a1', 'user@example.com', '$2a$10$dXJ3SWoGD7Y759vA1OBMse7Fi7W7QKBjY7B6M62aT81q5Zc8M15oG', '테스트유저')
ON DUPLICATE KEY UPDATE name=name;

-- 테스트 공연 2개
INSERT INTO shows (show_id, title, venue) 
VALUES (1, '임영웅 콘서트 IM HERO', '서울월드컵경기장')
ON DUPLICATE KEY UPDATE title=title;

INSERT INTO shows (show_id, title, venue) 
VALUES (2, '싸이 흠뻑쇼 SUMMERSWAG', '잠실종합운동장')
ON DUPLICATE KEY UPDATE title=title;

-- 공연 1번(임영웅 콘서트) 좌석 20개 자동 배치
INSERT INTO seats (seat_id, show_id, seat_number, price, status) VALUES
(1, 1, 'A-1', 150000, 'AVAILABLE'),
(2, 1, 'A-2', 150000, 'AVAILABLE'),
(3, 1, 'A-3', 150000, 'AVAILABLE'),
(4, 1, 'A-4', 150000, 'AVAILABLE'),
(5, 1, 'A-5', 150000, 'AVAILABLE'),
(6, 1, 'A-6', 150000, 'AVAILABLE'),
(7, 1, 'A-7', 150000, 'AVAILABLE'),
(8, 1, 'A-8', 150000, 'AVAILABLE'),
(9, 1, 'A-9', 150000, 'AVAILABLE'),
(10, 1, 'A-10', 150000, 'AVAILABLE'),
(11, 1, 'A-11', 150000, 'AVAILABLE'),
(12, 1, 'A-12', 150000, 'AVAILABLE'),
(13, 1, 'A-13', 150000, 'AVAILABLE'),
(14, 1, 'A-14', 150000, 'AVAILABLE'),
(15, 1, 'A-15', 150000, 'AVAILABLE'),
(16, 1, 'A-16', 150000, 'AVAILABLE'),
(17, 1, 'A-17', 150000, 'AVAILABLE'),
(18, 1, 'A-18', 150000, 'AVAILABLE'),
(19, 1, 'A-19', 150000, 'AVAILABLE'),
(20, 1, 'A-20', 150000, 'AVAILABLE')
ON DUPLICATE KEY UPDATE status=status;

-- 공연 2번(싸이 흠뻑쇼) 좌석 20개 자동 배치
INSERT INTO seats (seat_id, show_id, seat_number, price, status) VALUES
(21, 2, 'VIP-1', 165000, 'AVAILABLE'),
(22, 2, 'VIP-2', 165000, 'AVAILABLE'),
(23, 2, 'VIP-3', 165000, 'AVAILABLE'),
(24, 2, 'VIP-4', 165000, 'AVAILABLE'),
(25, 2, 'VIP-5', 165000, 'AVAILABLE'),
(26, 2, 'SR-1', 143000, 'AVAILABLE'),
(27, 2, 'SR-2', 143000, 'AVAILABLE'),
(28, 2, 'SR-3', 143000, 'AVAILABLE'),
(29, 2, 'SR-4', 143000, 'AVAILABLE'),
(30, 2, 'SR-5', 143000, 'AVAILABLE'),
(31, 2, 'R-1', 132000, 'AVAILABLE'),
(32, 2, 'R-2', 132000, 'AVAILABLE'),
(33, 2, 'R-3', 132000, 'AVAILABLE'),
(34, 2, 'R-4', 132000, 'AVAILABLE'),
(35, 2, 'R-5', 132000, 'AVAILABLE'),
(36, 2, 'S-1', 110000, 'AVAILABLE'),
(37, 2, 'S-2', 110000, 'AVAILABLE'),
(38, 2, 'S-3', 110000, 'AVAILABLE'),
(39, 2, 'S-4', 110000, 'AVAILABLE'),
(40, 2, 'S-5', 110000, 'AVAILABLE')
ON DUPLICATE KEY UPDATE status=status;
