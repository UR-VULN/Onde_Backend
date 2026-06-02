-- members 테이블의 name 컬럼을 nullable(NULL 허용)로 변경합니다.
ALTER TABLE members MODIFY name VARCHAR(50) NULL COMMENT '사용자 이름 또는 업체명';
