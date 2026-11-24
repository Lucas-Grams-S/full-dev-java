-- V18 (EXPAND): additive only. Old code keeps working; nothing is read from the new column yet.
ALTER TABLE users ADD COLUMN phone_e164 VARCHAR(16);

CREATE FUNCTION normalize_phone_br(raw text) RETURNS text LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
  SELECT CASE WHEN n ~ '^[1-9][0-9][2-9][0-9]{7}$' OR n ~ '^[1-9][0-9]9[0-9]{8}$' THEN '+55' || n END
  FROM (SELECT CASE WHEN d ~ '^55[1-9][0-9]{9,10}$' THEN substr(d, 3)
                    WHEN d ~ '^0[1-9][0-9]{9,10}$'  THEN substr(d, 2)
                    ELSE d END AS n
        FROM (SELECT regexp_replace(coalesce(raw, ''), '[^0-9]', '', 'g') AS d) x) y
$$;

-- keeps the new column in step with every write made by the OLD code (and by rolling-deploy instances still running it)
CREATE FUNCTION users_sync_phone_e164() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF TG_OP = 'INSERT' OR NEW.phone IS DISTINCT FROM OLD.phone THEN
    NEW.phone_e164 := normalize_phone_br(NEW.phone);
  END IF;
  RETURN NEW;
END $$;

CREATE TRIGGER trg_users_sync_phone BEFORE INSERT OR UPDATE OF phone ON users
  FOR EACH ROW EXECUTE FUNCTION users_sync_phone_e164();
