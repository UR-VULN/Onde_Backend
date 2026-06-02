-- Normalize remaining Hibernate-generated DATETIME(6) columns to the domain
-- specification's DATETIME type.

ALTER TABLE accommodations MODIFY submit_date DATETIME NOT NULL;

ALTER TABLE comments MODIFY created_at DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE comments MODIFY updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE reservations MODIFY check_in DATETIME NOT NULL;
ALTER TABLE reservations MODIFY check_out DATETIME NOT NULL;

ALTER TABLE settlements MODIFY requested_at DATETIME NULL;
ALTER TABLE settlements MODIFY approved_at DATETIME NULL;
ALTER TABLE settlements MODIFY finalized_at DATETIME NULL;
