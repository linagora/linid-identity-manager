CREATE TABLE IF NOT EXISTS account_status
(
    ast_id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    act_id         UUID         NOT NULL REFERENCES accounts (act_id) ON DELETE CASCADE,
    activated_at   TIMESTAMPTZ,
    deactivated_at TIMESTAMPTZ
        CHECK (
            deactivated_at IS NULL
            OR activated_at IS NULL
            OR deactivated_at >= activated_at
        ),
    suspended_from TIMESTAMPTZ
        CHECK (
            suspended_from IS NULL
            OR activated_at IS NOT NULL
        ),
    suspended_to TIMESTAMPTZ
        CHECK (
            suspended_to IS NULL
            OR suspended_from IS NULL
            OR suspended_to >= suspended_from
        ),
    created_by     VARCHAR(128) NOT NULL,
    updated_by     VARCHAR(128) NOT NULL,
    insert_date    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_date    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TRIGGER tg_account_status_set_update_date
    BEFORE UPDATE
    ON account_status
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE account_status IS 'Table storing the activation and deactivation timestamps of accounts, along with audit information (created_by, updated_by, insert_date, update_date).';

COMMENT ON COLUMN account_status.ast_id IS 'Primary key. UUID automatically generated for each account status record.';
COMMENT ON COLUMN account_status.act_id IS 'Foreign key referencing accounts(act_id). Links this status record to its account. ON DELETE CASCADE ensures the status is removed if the account is deleted.';
COMMENT ON COLUMN account_status.activated_at IS 'Date and time when the account was activated. Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN account_status.deactivated_at IS 'Date and time when the account was deactivated or suspended. Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN account_status.suspended_from IS 'Start timestamp of the suspension period. The account is considered suspended from this instant. Stored in UTC (TIMESTAMPTZ). Requires activated_at to be defined.';
COMMENT ON COLUMN account_status.suspended_to IS 'End timestamp of the suspension period. If NULL, the suspension is considered ongoing. Must be greater than or equal to suspended_from when both are defined. Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN account_status.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN account_status.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN account_status.insert_date IS 'Date and time when the account record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN account_status.update_date IS 'Date and time when the account record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON TRIGGER tg_account_status_set_update_date ON account_status IS 'Trigger that sets update_date to the current timestamp (NOW()) whenever a row in account_status is updated.';
