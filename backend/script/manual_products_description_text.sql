-- Allows manual product descriptions to store HTML rich text longer than VARCHAR(255).
-- Run once on databases that are not managed by Hibernate ddl-auto:update.
ALTER TABLE manual_products
MODIFY description TEXT;
