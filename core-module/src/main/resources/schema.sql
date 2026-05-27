-- 기존에 테이블이 존재할 경우를 대비해 초기화 세팅 (필요 시 사용)
-- DROP TABLE IF EXISTS members;

CREATE TABLE members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '회원 고유 식별자 (PK)',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '로그인 이메일 아이디',
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt 암호화된 비밀번호',
    name VARCHAR(50) NOT NULL COMMENT '사용자 이름 또는 업체명',
    phone_number VARCHAR(20) COMMENT '연락처',
    role VARCHAR(20) NOT NULL COMMENT '권한 등급 (USER, SELLER, ADMIN 등)',
    status VARCHAR(20) NOT NULL COMMENT '계정 상태 (ACTIVE, DORMANT, WITHDRAWN)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '최초 생성 일시',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '최종 수정 일시',
    
    -- 인가(Authorization) 및 권한별 조회 성능 최적화를 위한 복합 인덱스 설정
    INDEX idx_member_role (role),
    INDEX idx_member_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;