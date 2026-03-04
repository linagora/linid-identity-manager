CREATE TABLE IF NOT EXISTS accounts
(
    act_id      UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    external_id VARCHAR(128) NOT NULL,
    email       VARCHAR(320) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    payload     JSONB        NOT NULL DEFAULT '{}'::JSONB,
    checksum    VARCHAR(64)  NOT NULL,-- TODO: comment
    created_by  VARCHAR(128) NOT NULL,
    updated_by  VARCHAR(128) NOT NULL,
    insert_date TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_date TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_accounts_external_id ON accounts (external_id);

CREATE TRIGGER tg_accounts_set_update_date
    BEFORE UPDATE
    ON accounts
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE accounts IS 'Table storing account-related information, including internal and external identifiers, contact email, and audit data.';

COMMENT ON COLUMN accounts.act_id IS 'Primary key. UUID automatically generated for each account.';
COMMENT ON COLUMN accounts.external_id IS 'External identifier for the account, such as an OIDC sub or external system ID.';
COMMENT ON COLUMN accounts.email IS 'Email address associated with the account. Unique constraint enforced.';
COMMENT ON COLUMN accounts.name IS 'Full name of the account holder (first name + last name).';
COMMENT ON COLUMN accounts.payload IS 'JSONB column storing the user payload from external systems. Used for access control evaluation via OPA and for generating JWT claims.';
COMMENT ON COLUMN accounts.checksum IS 'Deterministic hash (e.g. SHA-256) computed from selected account fields (typically payload and/or external attributes). Used to detect changes, ensure data consistency, and avoid unnecessary downstream processing (e.g. JWT regeneration or policy reevaluation).';
COMMENT ON COLUMN accounts.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN accounts.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN accounts.insert_date IS 'Date and time when the account record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN accounts.update_date IS 'Date and time when the account record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON INDEX idx_accounts_external_id IS 'Unique index on external_id to enforce uniqueness and improve query performance when searching by external_id.';

COMMENT ON TRIGGER tg_accounts_set_update_date ON accounts IS 'Trigger that invokes the update_timestamp() function before each UPDATE to automatically set update_date to NOW().';
