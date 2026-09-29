CREATE TABLE group_accounts
(
    gra_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    grp_id           UUID        NOT NULL REFERENCES groups (grp_id) ON DELETE CASCADE,
    act_id           UUID        NOT NULL REFERENCES accounts (act_id) ON DELETE CASCADE,
    extra_parameters JSONB       NOT NULL DEFAULT '{}'::JSONB,
    created_by       UUID        NOT NULL,
    updated_by       UUID        NOT NULL,
    insert_date      TIMESTAMPTZ NOT NULL DEFAULT now(),
    update_date      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_group_accounts_grp_id_act_id UNIQUE (grp_id, act_id)
);

CREATE INDEX idx_group_accounts_grp_id ON group_accounts (grp_id);
CREATE INDEX idx_group_accounts_act_id ON group_accounts (act_id);

CREATE TRIGGER tg_group_accounts_set_update_date
    BEFORE UPDATE
    ON group_accounts
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE group_accounts IS 'Stores the association between groups and accounts.';

COMMENT ON COLUMN group_accounts.gra_id IS 'Primary key (UUID) of the group to account association.';
COMMENT ON COLUMN group_accounts.grp_id IS 'Identifier of the associated group.';
COMMENT ON COLUMN group_accounts.act_id IS 'Identifier of the associated account.';
COMMENT ON COLUMN group_accounts.extra_parameters IS 'JSONB column containing custom attributes and metadata defined by the deployment. Intended for customer-specific or integration-specific extensions that are not part of the standard data model.';
COMMENT ON COLUMN group_accounts.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN group_accounts.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN group_accounts.insert_date IS 'Date and time when the association record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN group_accounts.update_date IS 'Date and time when the association record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON CONSTRAINT uk_group_accounts_grp_id_act_id ON group_accounts IS 'Ensures that an account can only be associated once with a given group.';

COMMENT ON INDEX idx_group_accounts_grp_id IS 'Index on group identifier to optimize lookups and joins on group_accounts by group.';
COMMENT ON INDEX idx_group_accounts_act_id IS 'Index on account identifier to optimize lookups and joins on group_accounts by account.';

COMMENT ON TRIGGER tg_group_accounts_set_update_date ON group_accounts IS 'Trigger that invokes the update_timestamp() function before each UPDATE to automatically set update_date to NOW().';
