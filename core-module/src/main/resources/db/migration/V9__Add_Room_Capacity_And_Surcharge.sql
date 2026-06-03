ALTER TABLE rooms 
ADD COLUMN standard_capacity INT NOT NULL DEFAULT 2 AFTER capacity,
ADD COLUMN max_capacity INT NOT NULL DEFAULT 4 AFTER standard_capacity,
ADD COLUMN surcharge DECIMAL(12,2) NOT NULL DEFAULT 0.00 AFTER max_capacity;

-- Sync existing capacity to standard and max for safety
UPDATE rooms SET standard_capacity = capacity, max_capacity = capacity;
