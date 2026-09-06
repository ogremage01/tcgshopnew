-- Add OpenBinder image download state columns for validate/manual schemas.
ALTER TABLE mtg_prices
    ADD COLUMN downloaded BOOLEAN DEFAULT FALSE;

ALTER TABLE fab_prices
    ADD COLUMN downloaded BOOLEAN DEFAULT FALSE;
