CREATE TABLE group_managers
(
    grm_id      UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    grp_id      UUID        NOT NULL REFERENCES groups (grp_id) ON DELETE CASCADE,
    act_id      UUID        NOT NULL REFERENCES accounts (act_id) ON DELETE CASCADE,
    created_by  UUID        NOT NULL,
    updated_by  UUID        NOT NULL,
    insert_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    update_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_group_managers_grp_id_act_id UNIQUE (grp_id, act_id)
);

CREATE INDEX idx_group_managers_act_id ON group_managers (act_id);

CREATE TRIGGER tg_group_managers_set_update_date
    BEFORE UPDATE
    ON group_managers
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE group_managers IS 'Stores the managers of each group. A manager does not need to be a member of the group.';

COMMENT ON COLUMN group_managers.grm_id IS 'Primary key (UUID) of the group manager.';
COMMENT ON COLUMN group_managers.grp_id IS 'Identifier of the managed group.';
COMMENT ON COLUMN group_managers.act_id IS 'Identifier of the manager account.';
COMMENT ON COLUMN group_managers.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN group_managers.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN group_managers.insert_date IS 'Date and time when the manager was added. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN group_managers.update_date IS 'Date and time when the manager record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON CONSTRAINT uk_group_managers_grp_id_act_id ON group_managers IS 'Ensures that an account can only be a manager once of a given group. Its index also serves the lookups by group.';

COMMENT ON INDEX idx_group_managers_act_id IS 'Index on account identifier to optimize lookups of the groups managed by an account.';

COMMENT ON TRIGGER tg_group_managers_set_update_date ON group_managers IS 'Trigger that invokes the update_timestamp() function before each UPDATE to automatically set update_date to NOW().';
