CREATE TABLE IF NOT EXISTS items (
    id UUID PRIMARY KEY,
    item_name TEXT UNIQUE,
    item_description TEXT,
    item_image_location TEXT
);
