CREATE TABLE IF NOT EXISTS applications
(
    app_id       UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    app_key      VARCHAR(100) NOT NULL UNIQUE,
    auth_type    VARCHAR(50)  NOT NULL, -- TODO: limit type of auth from lemon
    auth_config  JSONB        NOT NULL DEFAULT '{}'::JSONB,
    jwt_template TEXT         NOT NULL DEFAULT '',
    created_by   VARCHAR(128) NOT NULL,
    updated_by   VARCHAR(128) NOT NULL,
    insert_date  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_date  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TRIGGER tg_applications_set_update_date
    BEFORE UPDATE
    ON applications
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE applications IS 'Table storing applications of the information system, including their technical identifier, authentication type, authentication configuration, and JWT claim template.';

COMMENT ON COLUMN applications.app_id IS 'Primary key. UUID automatically generated for each application.';
COMMENT ON COLUMN applications.app_key IS 'Technical unique identifier of the application. Must start and end with an uppercase alphanumeric character and may contain underscores in the middle (e.g., TEST_APP_BIDULE). Enforced via CHECK constraint.';
COMMENT ON COLUMN applications.auth_type IS 'Authentication mechanism configured in LemonLDAP::NG for this application (e.g., OIDC, CAS, SAML). Defines how LemonLDAP::NG will authenticate users when accessing the application.';
COMMENT ON COLUMN applications.auth_config IS 'JSONB configuration associated with the selected LemonLDAP::NG authentication mechanism. The expected structure depends on auth_type (e.g., OIDC client parameters, CAS service configuration, SAML metadata settings).';
COMMENT ON COLUMN applications.jwt_template IS 'Jinja2 template used to generate JWT claims for this application. The template is evaluated at runtime using account and context data.';
COMMENT ON COLUMN applications.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN applications.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN applications.insert_date IS 'Date and time when the account record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN applications.update_date IS 'Date and time when the account record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON TRIGGER tg_applications_set_update_date ON applications IS 'Trigger executed before each UPDATE to automatically set update_date to the current timestamp via update_timestamp() function.';
