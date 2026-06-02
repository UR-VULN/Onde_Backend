-- seller_accounts 테이블에 잘못 남아있을 수 있는 seller_id 컬럼을 제거하여 엔티티와 싱크를 맞춥니다.
ALTER TABLE seller_accounts DROP COLUMN IF EXISTS seller_id;
