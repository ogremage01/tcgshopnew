-- Optional: existing DBs with product_search_maps can add the catalog link used by CARD_PRODUCT rows.
-- Hibernate ddl-auto: update creates the column automatically in dev, but validate/manual schemas need this DDL.
ALTER TABLE product_search_maps
    ADD COLUMN catalog_source_id BIGINT NULL;

CREATE INDEX idx_product_search_maps_table_catalog_source_id
    ON product_search_maps (table_name, catalog_source_id);
