ALTER TABLE rooms
	ADD COLUMN room_kind VARCHAR(32) NOT NULL DEFAULT 'WATCH_PARTY';

COMMENT ON COLUMN rooms.room_kind IS 'WATCH_PARTY = movie night, MEET = meet & share';
