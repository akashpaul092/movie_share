ALTER TABLE rooms ADD COLUMN admin_secret VARCHAR(64);

UPDATE rooms
SET admin_secret = gen_random_uuid()::text
WHERE admin_secret IS NULL;

ALTER TABLE rooms ALTER COLUMN admin_secret SET NOT NULL;
