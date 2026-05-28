-- 1. 항공 노선 (ICN -> NRT) 더미데이터 삽입
INSERT INTO flight_routes (id, route_code, departure_airport, arrival_airport, distance_km, created_at, updated_at)
VALUES (1, 'ICN-NRT-01', 'ICN', 'NRT', 1000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE route_code = route_code;

-- 2. 항공 스케줄 더미데이터 삽입 (2026-06-15 출발 일정)
INSERT INTO flight_schedules (id, route_id, flight_number, departure_time, arrival_time, status, reject_reason, created_at, updated_at)
VALUES (1, 1, 'OZ302', '2026-06-15 09:00:00', '2026-06-15 11:30:00', 'APPROVED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE flight_number = flight_number;

-- 3. 좌석 재고 더미데이터 삽입 (ECONOMY, BUSINESS, FIRST)
INSERT INTO seat_inventories (id, flight_schedule_id, class_type, total_seats, remaining_seats, base_price, created_at, updated_at)
VALUES 
(1, 1, 'ECONOMY', 150, 150, 120000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, 'BUSINESS', 24, 24, 300000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, 'FIRST', 12, 12, 500000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE remaining_seats = remaining_seats;

-- 4. 결제/취소 테스트용 항공 임시 예약 더미데이터 삽입
INSERT INTO flight_bookings (id, booking_code, flight_schedule_id, user_id, passenger_name, passenger_passport, passenger_birthdate, seat_class, total_price, status, reserved_until, created_at, updated_at)
VALUES (1, 'BK-20260528-999', 1, 1, '홍길동', 'M12345678', '1995-08-15', 'ECONOMY', 120000.00, 'PENDING_PAYMENT', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 10 MINUTE), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE booking_code = booking_code;

-- 5. 보험 상품 더미데이터 삽입
INSERT INTO insurance_products (id, product_name, base_daily_rate, coverage_details, status, reject_reason, created_at, updated_at)
VALUES (1, '온데 안심 여행자 보험 v1', 1200.00, '{"medical": 50000000, "liability": 20000000, "baggage": 1000000}', 'APPROVED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE product_name = product_name;
