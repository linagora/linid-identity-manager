CREATE TABLE IF NOT EXISTS account_application_profiles
(
    aap_id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    act_id              UUID REFERENCES accounts (act_id) ON DELETE CASCADE,
    app_id              UUID REFERENCES applications (app_id) ON DELETE CASCADE,
    claims              JSONB        NOT NULL,
    entitlements        JSONB        NOT NULL,
    account_checksum    VARCHAR(64)  NOT NULL,
    opa_script_checksum VARCHAR(64)  NOT NULL,
    changed_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          VARCHAR(128) NOT NULL,
    updated_by          VARCHAR(128) NOT NULL,
    insert_date         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_date         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TRIGGER tg_account_application_profiles_set_update_date
    BEFORE UPDATE
    ON account_application_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE account_application_profiles IS 'Table storing the computed profile for each account and application pair, including evaluated JWT claims and OPA-derived entitlements. Acts as a cache of the last known state, traceable via checksums.';

COMMENT ON COLUMN account_application_profiles.aap_id IS 'Primary key. UUID automatically generated for each account application profile.';
COMMENT ON COLUMN account_application_profiles.act_id IS 'Foreign key referencing the account this profile belongs to. Cascades on delete.';
COMMENT ON COLUMN account_application_profiles.app_id IS 'Foreign key referencing the application this profile is computed for. Cascades on delete.';
COMMENT ON COLUMN account_application_profiles.claims IS 'JSONB payload containing the JWT claims evaluated for this account in the context of this application, based on the application JWT template.';
COMMENT ON COLUMN account_application_profiles.entitlements IS 'JSONB payload containing the rights and permissions computed by the OPA script for this account and application.';
COMMENT ON COLUMN account_application_profiles.account_checksum IS 'Checksum of the account data used during the last computation. Used to detect whether the profile needs to be recomputed due to account changes.';
COMMENT ON COLUMN account_application_profiles.opa_script_checksum IS 'Checksum of the OPA script used during the last computation. Used to detect whether the profile needs to be recomputed due to policy changes.';
COMMENT ON COLUMN account_application_profiles.changed_at IS 'Date and time when the computed profile last changed (claims or entitlements). Updated only when the result of the computation differs from the previous one.';
COMMENT ON COLUMN applications.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN applications.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN applications.insert_date IS 'Date and time when the account record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN applications.update_date IS 'Date and time when the account record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON TRIGGER tg_account_application_profiles_set_update_date ON account_application_profiles IS 'Trigger executed before each UPDATE to automatically set update_date to the current timestamp via update_timestamp() function.';
