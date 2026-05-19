CREATE TABLE IF NOT EXISTS organizational_unit_status
(
    ous_id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    oun_id            UUID         NOT NULL UNIQUE REFERENCES organizational_units (oun_id) ON DELETE CASCADE,
    suspension_period TSTZRANGE,
    status_reason     VARCHAR(250),
    status_subreason  VARCHAR(250),
    status_comment    TEXT,
    created_by        UUID         NOT NULL,
    updated_by        UUID         NOT NULL,
    insert_date       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_date       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_organizational_unit_status_suspension ON organizational_unit_status USING GIST (suspension_period);

CREATE TRIGGER tg_organizational_unit_status_set_update_date
    BEFORE UPDATE
    ON organizational_unit_status
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

-- ---------------------------------------------------------------------------
-- Automatic status initialization
-- ---------------------------------------------------------------------------
-- A status row must exist for every organizational unit. It is created at the
-- database level so it is consistent whether the unit is inserted through the
-- application layer or through direct SQL (migrations, demo / e2e seeds).
-- suspension_period is left NULL (no suspension) by default. The audit fields
-- fall back to a constant system UUID when the source unit has no creator /
-- updater (e.g. the bootstrap 'root' unit), since organizational_units allows
-- NULL there while organizational_unit_status requires NOT NULL.
CREATE OR REPLACE FUNCTION f_init_organizational_unit_status()
    RETURNS TRIGGER AS
$$
BEGIN
    INSERT INTO organizational_unit_status (oun_id, created_by, updated_by)
    VALUES (NEW.oun_id,
            COALESCE(NEW.created_by, NEW.updated_by, '00000000-0000-0000-0000-000000000000'),
            COALESCE(NEW.updated_by, NEW.created_by, '00000000-0000-0000-0000-000000000000'))
    ON CONFLICT (oun_id) DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_organizational_unit_status_init
    AFTER INSERT
    ON organizational_units
    FOR EACH ROW
EXECUTE FUNCTION f_init_organizational_unit_status();

COMMENT ON TABLE organizational_unit_status IS 'Table storing the suspension status of an organizational unit: optional suspension time range, status reason / sub-reason / comment, along with audit information (created_by, updated_by, insert_date, update_date). Exactly one row exists per organizational unit.';

COMMENT ON COLUMN organizational_unit_status.ous_id IS 'Primary key. UUID automatically generated for each organizational unit status record.';
COMMENT ON COLUMN organizational_unit_status.oun_id IS 'Foreign key referencing organizational_units(oun_id). Links this status record to its organizational unit. The UNIQUE constraint enforces a one-to-one relationship. ON DELETE CASCADE ensures the status is removed if the organizational unit is deleted.';
COMMENT ON COLUMN organizational_unit_status.suspension_period IS 'Time range during which the organizational unit is suspended. Lower bound represents the suspension start timestamp, and upper bound represents the suspension end timestamp. NULL when no suspension is configured. An open-ended suspension (NULL upper bound) is treated as a permanent suspension. Stored as tstzrange (TIMESTAMPTZ range) in UTC.';
COMMENT ON COLUMN organizational_unit_status.status_reason IS 'High-level reason explaining a suspension change.';
COMMENT ON COLUMN organizational_unit_status.status_subreason IS 'More detailed classification of the status reason.';
COMMENT ON COLUMN organizational_unit_status.status_comment IS 'Optional free-text comment providing additional context about the status change.';
COMMENT ON COLUMN organizational_unit_status.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN organizational_unit_status.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN organizational_unit_status.insert_date IS 'Date and time when the organizational unit status record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN organizational_unit_status.update_date IS 'Date and time when the organizational unit status record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON INDEX idx_organizational_unit_status_suspension IS 'GIST index on suspension_period to speed up range containment and overlap queries.';

COMMENT ON TRIGGER tg_organizational_unit_status_set_update_date ON organizational_unit_status IS 'Trigger that sets update_date to the current timestamp (NOW()) whenever a row in organizational_unit_status is updated.';

COMMENT ON FUNCTION f_init_organizational_unit_status() IS 'Trigger function that automatically inserts a default organizational_unit_status row (no suspension) whenever an organizational unit is created, falling back to a constant system UUID for audit fields when the source unit has no creator / updater.';

COMMENT ON TRIGGER tg_organizational_unit_status_init ON organizational_units IS 'Trigger that invokes f_init_organizational_unit_status() after every INSERT on organizational_units to guarantee a status row exists for each organizational unit.';
