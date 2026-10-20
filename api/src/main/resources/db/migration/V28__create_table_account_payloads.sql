CREATE TABLE IF NOT EXISTS account_payloads
(
    acp_id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    act_id                UUID         NOT NULL REFERENCES accounts (act_id) ON DELETE CASCADE,
    payload               JSONB        NOT NULL DEFAULT '{}'::JSONB,
    checksum              TEXT         NOT NULL,
    sync_instance_id      VARCHAR(255) NOT NULL,
    sync_job_execution_id BIGINT       NOT NULL,
    created_by            UUID         NOT NULL,
    updated_by            UUID         NOT NULL,
    insert_date           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_date           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_account_payloads_act_id UNIQUE (act_id)
);

CREATE TRIGGER tg_account_payloads_set_update_date
    BEFORE UPDATE
    ON account_payloads
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE account_payloads IS 'Table storing the payload of each account, collected from the sources by sync-api and used as OPA input, with its checksum and the sync-api run that wrote it.';

COMMENT ON COLUMN account_payloads.acp_id IS 'Primary key. UUID automatically generated for each account payload.';
COMMENT ON COLUMN account_payloads.act_id IS 'Identifier of the account the payload belongs to. Foreign key to accounts, cascaded on delete.';
COMMENT ON COLUMN account_payloads.payload IS 'JSONB data collected from the sources for the account, used as OPA input.';
COMMENT ON COLUMN account_payloads.checksum IS 'SHA-256 checksum (hexadecimal) of the payload, computed by sync-api on sorted values. Used to detect payload changes.';
COMMENT ON COLUMN account_payloads.sync_instance_id IS 'Identifier of the sync-api instance that wrote the row.';
COMMENT ON COLUMN account_payloads.sync_job_execution_id IS 'Identifier of the Spring Batch job execution that wrote the row.';
COMMENT ON COLUMN account_payloads.created_by IS 'Identifier of the technical account of sync-api that created this record.';
COMMENT ON COLUMN account_payloads.updated_by IS 'Identifier of the technical account of sync-api that last updated this record.';
COMMENT ON COLUMN account_payloads.insert_date IS 'Date and time when the account payload record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN account_payloads.update_date IS 'Date and time of the last payload change. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON CONSTRAINT uk_account_payloads_act_id ON account_payloads IS 'Ensures that an account has at most one payload. Its index also serves the lookups by account.';

COMMENT ON TRIGGER tg_account_payloads_set_update_date ON account_payloads IS 'Trigger that invokes the update_timestamp() function before each UPDATE to automatically set update_date to NOW().';
