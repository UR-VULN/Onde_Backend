ALTER TABLE seller_accounts DROP COLUMN IF EXISTS updated_at;

ALTER TABLE inventory DROP COLUMN IF EXISTS created_at;
ALTER TABLE inventory DROP COLUMN IF EXISTS updated_at;
