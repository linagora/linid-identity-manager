CREATE TABLE IF NOT EXISTS application_roles
(
    aro_id      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    app_id      UUID         NOT NULL REFERENCES applications (app_id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    created_by  UUID,
    updated_by  UUID,
    insert_date TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_date TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_application_roles_app_id_name UNIQUE (app_id, name)
);

CREATE INDEX idx_application_roles_app_id ON application_roles (app_id);

CREATE TRIGGER tg_application_roles_set_update_date
    BEFORE UPDATE
    ON application_roles
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE application_roles IS 'Table storing the roles exposed by an application, each identified by a name unique within the application, with audit data.';

COMMENT ON COLUMN application_roles.aro_id IS 'Primary key. UUID automatically generated for each application role.';
COMMENT ON COLUMN application_roles.app_id IS 'Identifier of the application the role belongs to. Foreign key to applications, cascaded on delete.';
COMMENT ON COLUMN application_roles.name IS 'Human-readable name of the role, unique within a given application.';
COMMENT ON COLUMN application_roles.description IS 'Optional free-text description of the role.';
COMMENT ON COLUMN application_roles.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN application_roles.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN application_roles.insert_date IS 'Date and time when the application role record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN application_roles.update_date IS 'Date and time when the application role record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON CONSTRAINT uk_application_roles_app_id_name ON application_roles IS 'Ensures that a role name is unique within a given application.';

COMMENT ON INDEX idx_application_roles_app_id IS 'Index on application identifier to optimize lookups and joins on application_roles by application.';

COMMENT ON TRIGGER tg_application_roles_set_update_date ON application_roles IS 'Trigger that invokes the update_timestamp() function before each UPDATE to automatically set update_date to NOW().';
