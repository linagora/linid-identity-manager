CREATE TABLE IF NOT EXISTS user_preferences
(
    usp_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(320) NOT NULL,
    key         TEXT NOT NULL,
    value       TEXT NOT NULL,
    created_by  UUID NOT NULL,
    updated_by  UUID NOT NULL,
    insert_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    update_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_user_preferences_email_key UNIQUE (email, key)
);

CREATE INDEX idx_user_preferences_email ON user_preferences (email);

CREATE TRIGGER tg_user_preferences_set_update_date
    BEFORE UPDATE
    ON user_preferences
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE user_preferences IS 'Stores per-user key/value preferences. Each user (identified by email) can have at most one value per key.';

COMMENT ON COLUMN user_preferences.usp_id IS 'Primary key. UUID automatically generated for each preference record.';
COMMENT ON COLUMN user_preferences.email IS 'Email address of the account owning this preference. Resolved from the JWT security context (UserPrincipal); never passed through the API.';
COMMENT ON COLUMN user_preferences.key IS 'Preference key (e.g. theme, language). Unique per user.';
COMMENT ON COLUMN user_preferences.value IS 'Preference value stored as plain text. No type coercion at persistence level.';
COMMENT ON COLUMN user_preferences.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN user_preferences.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN user_preferences.insert_date IS 'Date and time when the preference record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN user_preferences.update_date IS 'Date and time when the preference record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON CONSTRAINT uk_user_preferences_email_key ON user_preferences IS 'Enforces business uniqueness: one preference value per key per user (identified by email). Backs the UPSERT (ON CONFLICT) on POST /user-preferences.';

COMMENT ON INDEX idx_user_preferences_email IS 'Index on email to optimize lookups of all preferences belonging to a user.';

COMMENT ON TRIGGER tg_user_preferences_set_update_date ON user_preferences IS 'Trigger that invokes the update_timestamp() function before each UPDATE to automatically set update_date to NOW().';
