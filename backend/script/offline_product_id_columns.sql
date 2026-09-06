ALTER TABLE sealed_product    ADD COLUMN IF NOT EXISTS offline_product_id VARCHAR(255) NULL;
ALTER TABLE manual_products   ADD COLUMN IF NOT EXISTS offline_product_id VARCHAR(255) NULL;
ALTER TABLE supplies          ADD COLUMN IF NOT EXISTS offline_product_id VARCHAR(255) NULL;
