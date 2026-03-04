CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE OR REPLACE FUNCTION update_timestamp()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.update_date = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION update_timestamp() IS 'Trigger function that sets the update_date column to the current timestamp (NOW()) whenever a row in a table is updated.';

CREATE ROLE audit_trigger_role NOLOGIN;
