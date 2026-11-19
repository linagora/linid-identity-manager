CREATE TABLE application_managers
(
    apm_id      UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    app_id      UUID        NOT NULL REFERENCES applications (app_id) ON DELETE CASCADE,
    act_id      UUID        NOT NULL REFERENCES accounts (act_id) ON DELETE CASCADE,
    created_by  UUID        NOT NULL,
    updated_by  UUID        NOT NULL,
    insert_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    update_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_application_managers_app_id_act_id UNIQUE (app_id, act_id)
);

CREATE INDEX idx_application_managers_act_id ON application_managers (act_id);

CREATE TRIGGER tg_application_managers_set_update_date
    BEFORE UPDATE
    ON application_managers
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE application_managers IS 'Stores the managers of each application.';

COMMENT ON COLUMN application_managers.apm_id IS 'Primary key (UUID) of the application manager.';
COMMENT ON COLUMN application_managers.app_id IS 'Identifier of the managed application.';
COMMENT ON COLUMN application_managers.act_id IS 'Identifier of the manager account.';
COMMENT ON COLUMN application_managers.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN application_managers.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN application_managers.insert_date IS 'Date and time when the manager was added. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN application_managers.update_date IS 'Date and time when the manager record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON CONSTRAINT uk_application_managers_app_id_act_id ON application_managers IS 'Ensures that an account can only be a manager once of a given application. Its index also serves the lookups by application.';

COMMENT ON INDEX idx_application_managers_act_id IS 'Index on account identifier to optimize lookups of the applications managed by an account.';

COMMENT ON TRIGGER tg_application_managers_set_update_date ON application_managers IS 'Trigger that invokes the update_timestamp() function before each UPDATE to automatically set update_date to NOW().';
