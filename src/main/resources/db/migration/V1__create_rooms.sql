CREATE TABLE rooms (
    id UUID PRIMARY KEY,
    code VARCHAR(8) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    name VARCHAR(255)
);

CREATE INDEX idx_rooms_code ON rooms (code);
