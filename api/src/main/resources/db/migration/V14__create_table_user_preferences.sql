CREATE TABLE user_preferences
(
    usp_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    act_id      UUID NOT NULL REFERENCES accounts (act_id) ON DELETE CASCADE,
    usp_key     TEXT NOT NULL,
    usp_value   TEXT NOT NULL,
    created_by  UUID NOT NULL,
    updated_by  UUID NOT NULL,
    insert_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    update_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_user_preferences_act_id_usp_key UNIQUE (act_id, usp_key)
);

CREATE INDEX idx_user_preferences_act_id ON user_preferences (act_id);

CREATE TRIGGER tg_user_preferences_set_update_date
    BEFORE UPDATE
    ON user_preferences
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE user_preferences IS 'Stores per-user key/value preferences. Each user (identified by act_id) can have at most one value per key.';

COMMENT ON COLUMN user_preferences.usp_id IS 'Primary key. UUID automatically generated for each preference record.';
COMMENT ON COLUMN user_preferences.act_id IS 'Foreign key referencing accounts(act_id). Links this user preference record to its account. ON DELETE CASCADE ensures the status is removed if the account is deleted.';
COMMENT ON COLUMN user_preferences.usp_key IS 'Preference key (e.g. theme, language). Unique per user.';
COMMENT ON COLUMN user_preferences.usp_value IS 'Preference value stored as plain text. No type coercion at persistence level.';
COMMENT ON COLUMN user_preferences.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN user_preferences.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN user_preferences.insert_date IS 'Date and time when the preference record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN user_preferences.update_date IS 'Date and time when the preference record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON CONSTRAINT uk_user_preferences_act_id_usp_key ON user_preferences IS 'Enforces business uniqueness: one preference value per key per user (identified by act_id).';

COMMENT ON INDEX idx_user_preferences_act_id IS 'Index on act_id to optimize lookups of all preferences belonging to a user.';

COMMENT ON TRIGGER tg_user_preferences_set_update_date ON user_preferences IS 'Trigger that invokes the update_timestamp() function before each UPDATE to automatically set update_date to NOW().';
