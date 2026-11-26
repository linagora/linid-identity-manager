CREATE TABLE organizational_unit_managers
(
    oum_id      UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    oun_id      UUID        NOT NULL REFERENCES organizational_units (oun_id) ON DELETE CASCADE,
    act_id      UUID        NOT NULL REFERENCES accounts (act_id) ON DELETE CASCADE,
    created_by  UUID        NOT NULL,
    updated_by  UUID        NOT NULL,
    insert_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    update_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_organizational_unit_managers_oun_id_act_id UNIQUE (oun_id, act_id)
);

CREATE INDEX idx_organizational_unit_managers_act_id ON organizational_unit_managers (act_id);

CREATE TRIGGER tg_organizational_unit_managers_set_update_date
    BEFORE UPDATE
    ON organizational_unit_managers
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE organizational_unit_managers IS 'Stores the managers of each organizational unit. A manager does not need to be attached to the organizational unit as an account.';

COMMENT ON COLUMN organizational_unit_managers.oum_id IS 'Primary key (UUID) of the organizational unit manager.';
COMMENT ON COLUMN organizational_unit_managers.oun_id IS 'Identifier of the managed organizational unit.';
COMMENT ON COLUMN organizational_unit_managers.act_id IS 'Identifier of the manager account.';
COMMENT ON COLUMN organizational_unit_managers.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN organizational_unit_managers.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN organizational_unit_managers.insert_date IS 'Date and time when the manager was added. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN organizational_unit_managers.update_date IS 'Date and time when the manager record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON CONSTRAINT uk_organizational_unit_managers_oun_id_act_id ON organizational_unit_managers IS 'Ensures that an account can only be a manager once of a given organizational unit. Its index also serves the lookups by organizational unit.';

COMMENT ON INDEX idx_organizational_unit_managers_act_id IS 'Index on account identifier to optimize lookups of the organizational units managed by an account.';

COMMENT ON TRIGGER tg_organizational_unit_managers_set_update_date ON organizational_unit_managers IS 'Trigger that invokes the update_timestamp() function before each UPDATE to automatically set update_date to NOW().';
