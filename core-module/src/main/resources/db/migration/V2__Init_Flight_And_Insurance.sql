-- 1. 항공 노선 마스터 테이블
CREATE TABLE IF NOT EXISTS flight_routes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_code VARCHAR(20) UNIQUE NOT NULL,
    departure_airport VARCHAR(10) NOT NULL,
    arrival_airport VARCHAR(10) NOT NULL,
    distance_km INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 항공 스케줄 테이블
CREATE TABLE IF NOT EXISTS flight_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id BIGINT,
    flight_number VARCHAR(10) NOT NULL,
    departure_time DATETIME NOT NULL,
    arrival_time DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_APPROVAL',
    reject_reason TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (route_id) REFERENCES flight_routes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 좌석 등급별 실시간 재고 테이블
CREATE TABLE IF NOT EXISTS seat_inventories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flight_schedule_id BIGINT,
    class_type VARCHAR(15) NOT NULL,
    total_seats INT NOT NULL,
    remaining_seats INT NOT NULL,
    base_price DECIMAL(12, 2) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_schedule_class (flight_schedule_id, class_type),
    FOREIGN KEY (flight_schedule_id) REFERENCES flight_schedules(id) ON DELETE CASCADE,
    CONSTRAINT check_remaining_seats CHECK (remaining_seats >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 항공 예약 마스터 테이블
CREATE TABLE IF NOT EXISTS flight_bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_code VARCHAR(30) UNIQUE NOT NULL,
    flight_schedule_id BIGINT,
    user_id BIGINT NOT NULL,
    passenger_name VARCHAR(100) NOT NULL,
    passenger_passport VARCHAR(50) NOT NULL,
    passenger_birthdate DATE NOT NULL,
    seat_class VARCHAR(15) NOT NULL,
    total_price DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    reserved_until DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (flight_schedule_id) REFERENCES flight_schedules(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 보험 상품 마스터 테이블
CREATE TABLE IF NOT EXISTS insurance_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    base_daily_rate DECIMAL(12, 2) NOT NULL,
    coverage_details JSON NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_APPROVAL',
    reject_reason TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 여행자 보험 가입(계약) 테이블
CREATE TABLE IF NOT EXISTS insurance_policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_code VARCHAR(30) UNIQUE NOT NULL,
    insurance_product_id BIGINT,
    user_id BIGINT NOT NULL,
    insured_name VARCHAR(100) NOT NULL,
    insured_birthdate DATE NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    coverage_level VARCHAR(20) NOT NULL,
    total_premium DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (insurance_product_id) REFERENCES insurance_products(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. 고빈도 조작 인덱스 설계
CREATE INDEX IF NOT EXISTS idx_flight_search ON flight_schedules (departure_time, route_id, flight_number);
CREATE INDEX IF NOT EXISTS idx_flight_routes_airports ON flight_routes (departure_airport, arrival_airport);
CREATE INDEX IF NOT EXISTS idx_seat_inventory_schedule ON seat_inventories (flight_schedule_id, class_type);
CREATE INDEX IF NOT EXISTS idx_flight_bookings_status_expire ON flight_bookings (status, reserved_until);
