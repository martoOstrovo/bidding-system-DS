CREATE TABLE IF NOT EXISTS bids (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL,
    owner_id VARCHAR(255),
    highest_bidder_id VARCHAR(255),
    starting_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    current_bid NUMERIC(19, 2) NOT NULL DEFAULT 0,
    expiration_date TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Preserve existing listings. Older listings had no monetary amount, so their opening price is zero.
ALTER TABLE bids ADD COLUMN IF NOT EXISTS starting_price NUMERIC(19, 2) NOT NULL DEFAULT 0;
ALTER TABLE bids ADD COLUMN IF NOT EXISTS current_bid NUMERIC(19, 2) NOT NULL DEFAULT 0;
ALTER TABLE bids ALTER COLUMN highest_bidder_id SET DATA TYPE VARCHAR(255);
ALTER TABLE bids ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'OPEN';
CREATE INDEX IF NOT EXISTS bids_expiration_status_idx ON bids (status, expiration_date);
