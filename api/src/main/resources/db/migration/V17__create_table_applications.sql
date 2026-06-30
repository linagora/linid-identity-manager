CREATE TABLE IF NOT EXISTS applications
(
    app_id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(100) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    type            VARCHAR(255) NOT NULL,
    claims_template TEXT         NOT NULL,
    script          TEXT,
    script_checksum TEXT,
    deployed_at     DATE,
    configuration   JSONB,
    roles           JSONB,
    created_by      UUID         NOT NULL,
    updated_by      UUID         NOT NULL,
    insert_date     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_date     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_applications_code ON applications (code);

CREATE TRIGGER tg_applications_set_update_date
    BEFORE UPDATE
    ON applications
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE applications IS 'Table storing application definitions, including identification, claims template, optional provisioning script and configuration, and audit data.';

COMMENT ON COLUMN applications.app_id IS 'Primary key. UUID automatically generated for each application.';
COMMENT ON COLUMN applications.code IS 'Functional unique identifier of the application. Unique constraint enforced.';
COMMENT ON COLUMN applications.name IS 'Human-readable name of the application.';
COMMENT ON COLUMN applications.description IS 'Optional free-text description of the application.';
COMMENT ON COLUMN applications.type IS 'Type of the application.';
COMMENT ON COLUMN applications.claims_template IS 'Template used to generate the claims exposed to the application.';
COMMENT ON COLUMN applications.script IS 'Optional OPA Rego policy script stored to compute the access rights of the application.';
COMMENT ON COLUMN applications.script_checksum IS 'Deterministic hash (e.g. SHA-256) computed from the script. Used to detect changes to the script. NULL when no script is defined.';
COMMENT ON COLUMN applications.deployed_at IS 'Optional date when the application script was deployed on OPA.';
COMMENT ON COLUMN applications.configuration IS 'JSONB column storing the application-specific configuration.';
COMMENT ON COLUMN applications.roles IS 'JSONB array of strings storing the application roles.';
COMMENT ON COLUMN applications.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN applications.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN applications.insert_date IS 'Date and time when the application record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN applications.update_date IS 'Date and time when the application record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON INDEX idx_applications_code IS 'Unique index on code to enforce uniqueness and improve query performance when searching by code.';

COMMENT ON TRIGGER tg_applications_set_update_date ON applications IS 'Trigger that invokes the update_timestamp() function before each UPDATE to automatically set update_date to NOW().';
