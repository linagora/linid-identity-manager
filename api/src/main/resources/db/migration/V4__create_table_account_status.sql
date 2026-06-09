CREATE TABLE IF NOT EXISTS account_status
(
    ast_id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    act_id            UUID         NOT NULL REFERENCES accounts (act_id) ON DELETE CASCADE,
    validity_period   TSTZRANGE,
    suspension_period TSTZRANGE,
    activation_at     TIMESTAMPTZ,
    suspension_reason     VARCHAR(250),
    suspension_subreason  VARCHAR(250),
    suspension_comment    TEXT,
    deactivation_reason     VARCHAR(250),
    deactivation_subreason  VARCHAR(250),
    deactivation_comment    TEXT,
    reactivation_comment    TEXT,
    created_by        UUID         NOT NULL,
    updated_by        UUID         NOT NULL,
    insert_date       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_date       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_account_status_act_id ON account_status (act_id);
CREATE INDEX idx_account_status_validity ON account_status USING GIST (validity_period);
CREATE INDEX idx_account_status_suspension ON account_status USING GIST (suspension_period);

CREATE TRIGGER tg_account_status_set_update_date
    BEFORE UPDATE
    ON account_status
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE account_status IS 'Table storing the lifecycle status of an account: validity and suspension time ranges, activation timestamp, per-action reason / sub-reason / comment (suspension, deactivation, reactivation), along with audit information (created_by, updated_by, insert_date, update_date).';

COMMENT ON COLUMN account_status.ast_id IS 'Primary key. UUID automatically generated for each account status record.';
COMMENT ON COLUMN account_status.act_id IS 'Foreign key referencing accounts(act_id). Links this status record to its account. ON DELETE CASCADE ensures the status is removed if the account is deleted.';
COMMENT ON COLUMN account_status.validity_period IS 'Time range during which the account is active. Lower bound represents the activation timestamp, and upper bound represents the deactivation timestamp. Stored as tstzrange (TIMESTAMPTZ range) in UTC.';
COMMENT ON COLUMN account_status.suspension_period IS 'Time range during which the account is suspended. Lower bound represents the suspension start timestamp, and upper bound represents the suspension end timestamp. Stored as tstzrange (TIMESTAMPTZ range) in UTC.';
COMMENT ON COLUMN account_status.activation_at IS 'Timestamp when the account was activated or reactivated. Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN account_status.suspension_reason IS 'High-level reason explaining a suspension.';
COMMENT ON COLUMN account_status.suspension_subreason IS 'More detailed classification of the suspension reason.';
COMMENT ON COLUMN account_status.suspension_comment IS 'Optional free-text comment providing additional context about the suspension.';
COMMENT ON COLUMN account_status.deactivation_reason IS 'High-level reason explaining a deactivation.';
COMMENT ON COLUMN account_status.deactivation_subreason IS 'More detailed classification of the deactivation reason.';
COMMENT ON COLUMN account_status.deactivation_comment IS 'Optional free-text comment providing additional context about the deactivation.';
COMMENT ON COLUMN account_status.reactivation_comment IS 'Optional free-text comment providing additional context about a reactivation.';
COMMENT ON COLUMN account_status.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN account_status.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN account_status.insert_date IS 'Date and time when the account status record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN account_status.update_date IS 'Date and time when the account status record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON INDEX idx_account_status_act_id IS 'Unique index on act_id enforcing a one-to-one relationship between an account and its status record.';
COMMENT ON INDEX idx_account_status_validity IS 'GIST index on validity_period to speed up range containment and overlap queries.';
COMMENT ON INDEX idx_account_status_suspension IS 'GIST index on suspension_period to speed up range containment and overlap queries.';

COMMENT ON TRIGGER tg_account_status_set_update_date ON account_status IS 'Trigger that sets update_date to the current timestamp (NOW()) whenever a row in account_status is updated.';
