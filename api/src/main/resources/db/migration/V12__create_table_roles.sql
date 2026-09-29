CREATE TABLE IF NOT EXISTS roles
(
    rol_id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code             VARCHAR(100) NOT NULL UNIQUE,
    name             VARCHAR(255) NOT NULL,
    description      TEXT,
    extra_parameters JSONB        NOT NULL DEFAULT '{}'::JSONB,
    created_by       UUID         NOT NULL,
    updated_by       UUID         NOT NULL,
    insert_date      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_date      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_roles_code ON roles (code);

CREATE TRIGGER tg_roles_set_update_date
    BEFORE UPDATE
    ON roles
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE roles IS 'Table storing functional roles used to organize permissions, responsibilities, or business functions within the identity management system.';

COMMENT ON COLUMN roles.rol_id IS 'Primary key. UUID automatically generated for each role.';
COMMENT ON COLUMN roles.code IS 'Unique technical identifier of the role. Used for integrations, API references, and configuration.';
COMMENT ON COLUMN roles.name IS 'Human-readable name of the role displayed in the user interface.';
COMMENT ON COLUMN roles.description IS 'Optional description providing additional details about the purpose and usage of the role.';
COMMENT ON COLUMN roles.extra_parameters IS 'JSONB column containing custom attributes and metadata defined by the deployment. Intended for customer-specific or integration-specific extensions that are not part of the standard role data model.';
COMMENT ON COLUMN roles.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN roles.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN roles.insert_date IS 'Date and time when the record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN roles.update_date IS 'Date and time when the record was last updated. Automatically updated by trigger. Stored in UTC (TIMESTAMPTZ).';

COMMENT ON INDEX idx_roles_code IS 'Unique index enforcing role code uniqueness and improving lookup performance by role code.';

COMMENT ON TRIGGER tg_roles_set_update_date ON roles IS 'Trigger that invokes update_timestamp() before each UPDATE statement to automatically refresh update_date.';
