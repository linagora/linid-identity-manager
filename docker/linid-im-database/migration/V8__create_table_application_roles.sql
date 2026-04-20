CREATE TABLE IF NOT EXISTS application_roles
(
    apr_id       UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    app_id       UUID         NOT NULL REFERENCES applications (app_id) ON DELETE CASCADE,
    role_key     VARCHAR(100) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    created_by   VARCHAR(128) NOT NULL,
    updated_by   VARCHAR(128) NOT NULL,
    insert_date  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_date  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uc_application_roles_app_id_role_key UNIQUE (app_id, role_key)
);

-- Trigger pour mise à jour automatique
CREATE TRIGGER tg_application_roles_set_update_date
    BEFORE UPDATE
    ON application_roles
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE application_roles IS 'Table storing the roles available for each application. Each role is linked to a single application.';

COMMENT ON COLUMN application_roles.apr_id IS 'Primary key. UUID automatically generated for each role record.';
COMMENT ON COLUMN application_roles.app_id IS 'Foreign key referencing the associated application. Cascades on delete to remove roles if the application is deleted.';
COMMENT ON COLUMN application_roles.role_key IS 'Technical identifier of the role. Must start and end with an uppercase alphanumeric character and may contain underscores in the middle (e.g., ADMIN_USER).';
COMMENT ON COLUMN application_roles.display_name IS 'Human-readable name of the role. Can be used in the UI or for documentation.';
COMMENT ON COLUMN application_roles.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN application_roles.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN application_roles.insert_date IS 'Date and time when the account record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN application_roles.update_date IS 'Date and time when the account record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON CONSTRAINT uc_application_roles_app_id_role_key ON application_roles IS 'Ensures that within a single application, role_key values are unique.';

COMMENT ON TRIGGER tg_application_roles_set_update_date ON application_roles IS 'Trigger executed before each UPDATE to automatically set update_date to the current timestamp via update_timestamp() function.';
