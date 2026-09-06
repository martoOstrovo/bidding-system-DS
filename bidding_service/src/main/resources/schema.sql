CREATE TABLE IF NOT EXISTS bids (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL,
    highest_bidder_id UUID,
    expiration_date TIMESTAMP WITH TIME ZONE NOT NULL
);
