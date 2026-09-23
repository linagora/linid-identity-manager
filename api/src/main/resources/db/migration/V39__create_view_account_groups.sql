CREATE VIEW account_groups_view AS
SELECT
    group_accounts.act_id,
    groups_view.grp_id,
    groups_view.code,
    groups_view.name,
    groups_view.parent_id,
    groups_view.parent_name,
    groups_view.description,
    groups_view.email,
    groups_view.oun_id,
    groups_view.organizational_unit_name,
    groups_view.app_id,
    groups_view.application_name,
    group_accounts.extra_parameters AS "relation_extra_parameters",
    NULLIF(CONCAT_WS(' ', creator.firstname, creator.lastname), '') AS created_by,
    NULLIF(CONCAT_WS(' ', updater.firstname, updater.lastname), '') AS updated_by,
    group_accounts.insert_date,
    group_accounts.update_date
FROM
    group_accounts
JOIN groups_view
    ON groups_view.grp_id = group_accounts.grp_id
LEFT OUTER JOIN accounts creator
    ON creator.act_id = group_accounts.created_by
LEFT OUTER JOIN accounts updater
    ON updater.act_id = group_accounts.updated_by;

COMMENT ON VIEW account_groups_view IS 'Provides the groups each account is attached to, enriched with the group code, name, parent group, associated organizational unit and application, and with the audit information of the relationship. Holds one row per (account, group) pair; accounts without any membership are absent.';

COMMENT ON COLUMN account_groups_view.act_id IS 'Identifier of the account attached to the group.';
COMMENT ON COLUMN account_groups_view.grp_id IS 'Unique identifier of the group.';
COMMENT ON COLUMN account_groups_view.code IS 'Functional unique identifier of the group.';
COMMENT ON COLUMN account_groups_view.name IS 'Human-readable name of the group.';
COMMENT ON COLUMN account_groups_view.parent_id IS 'Identifier of the parent group. NULL when the group has no parent.';
COMMENT ON COLUMN account_groups_view.parent_name IS 'Name of the parent group. NULL when the group has no parent.';
COMMENT ON COLUMN account_groups_view.description IS 'Free-text description of the group.';
COMMENT ON COLUMN account_groups_view.email IS 'Email address of the group.';
COMMENT ON COLUMN account_groups_view.oun_id IS 'Identifier of the organizational unit associated with the group. NULL when the group has none.';
COMMENT ON COLUMN account_groups_view.organizational_unit_name IS 'Name of the organizational unit associated with the group. NULL when the group has none.';
COMMENT ON COLUMN account_groups_view.app_id IS 'Identifier of the application associated with the group. NULL when the group has none.';
COMMENT ON COLUMN account_groups_view.application_name IS 'Name of the application associated with the group. NULL when the group has none.';
COMMENT ON COLUMN account_groups_view.relation_extra_parameters IS 'JSONB column containing custom attributes and metadata associated with the relationship between an account and a group. Intended for customer-specific or integration-specific extensions that are not part of the standard data model.';
COMMENT ON COLUMN account_groups_view.created_by IS 'Full name of the account that attached the account to the group, formatted as "firstname lastname". NULL when the referenced account no longer exists.';
COMMENT ON COLUMN account_groups_view.updated_by IS 'Full name of the account that last updated the relationship, formatted as "firstname lastname". NULL when the referenced account no longer exists.';
COMMENT ON COLUMN account_groups_view.insert_date IS 'Date and time when the account was attached to the group. Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN account_groups_view.update_date IS 'Date and time when the relationship was last updated. Stored in UTC (TIMESTAMPTZ).';
