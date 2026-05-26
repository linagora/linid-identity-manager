CREATE TABLE organizational_unit_accounts
(
    oua_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oun_id      UUID NOT NULL REFERENCES organizational_units (oun_id) ON DELETE CASCADE,
    act_id      UUID NOT NULL REFERENCES accounts (act_id) ON DELETE CASCADE,
    created_by  UUID NOT NULL,
    updated_by  UUID NOT NULL,
    insert_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    update_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_organizational_unit_accounts_oun_id_act_id UNIQUE (oun_id, act_id)
);

CREATE INDEX idx_organizational_unit_accounts_oun_id ON organizational_unit_accounts (oun_id);
CREATE INDEX idx_organizational_unit_accounts_act_id ON organizational_unit_accounts (act_id);

CREATE TRIGGER tg_organizational_unit_accounts_set_update_date
    BEFORE UPDATE
    ON organizational_unit_accounts
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE organizational_unit_accounts IS 'Stores the association between organizational units and accounts.';

COMMENT ON COLUMN organizational_unit_accounts.oua_id IS 'Primary key (UUID) of the organizational unit to account association.';
COMMENT ON COLUMN organizational_unit_accounts.oun_id IS 'Identifier of the associated organizational unit.';
COMMENT ON COLUMN organizational_unit_accounts.act_id IS 'Identifier of the associated account.';
COMMENT ON COLUMN organizational_unit_accounts.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN organizational_unit_accounts.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN organizational_unit_accounts.insert_date IS 'Date and time when the association record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN organizational_unit_accounts.update_date IS 'Date and time when the association record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON CONSTRAINT uk_organizational_unit_accounts_oun_id_act_id ON organizational_unit_accounts IS 'Ensures that an account can only be associated once with a given organizational unit.';

COMMENT ON INDEX idx_organizational_unit_accounts_oun_id IS 'Index on organizational unit identifier to optimize lookups and joins on organizational_unit_accounts by OU.';
COMMENT ON INDEX idx_organizational_unit_accounts_act_id IS 'Index on account identifier to optimize lookups and joins on organizational_unit_accounts by account.';

COMMENT ON TRIGGER tg_organizational_unit_accounts_set_update_date ON organizational_unit_accounts IS 'Trigger that invokes the update_timestamp() function before each UPDATE to automatically set update_date to NOW().';
