CREATE VIEW group_accounts_view AS
SELECT DISTINCT
    group_accounts.grp_id,
    accounts_view.act_id,
    accounts_view.external_id,
    accounts_view.email,
    accounts_view.lastname,
    accounts_view.firstname,
    accounts_view.status,
    accounts_view.extra_parameters,
    group_accounts.extra_parameters AS "relation_extra_parameters",
    NULLIF(CONCAT_WS(' ', creator.firstname, creator.lastname), '') AS created_by,
    NULLIF(CONCAT_WS(' ', updater.firstname, updater.lastname), '') AS updated_by,
    group_accounts.insert_date,
    group_accounts.update_date
FROM
    group_accounts
JOIN accounts_view
    ON accounts_view.act_id = group_accounts.act_id
LEFT OUTER JOIN accounts creator
    ON creator.act_id = group_accounts.created_by
LEFT OUTER JOIN accounts updater
    ON updater.act_id = group_accounts.updated_by;

COMMENT ON VIEW group_accounts_view IS 'Provides the accounts attached to each group, enriched with their identity attributes, their computed lifecycle status and the audit information of the relationship. Holds one row per (group, account) pair; accounts without any group membership are absent. DISTINCT collapses the rows accounts_view emits for each organizational unit of a multi-membership account.';

COMMENT ON COLUMN group_accounts_view.grp_id IS 'Identifier of the group the account is attached to.';
COMMENT ON COLUMN group_accounts_view.act_id IS 'Unique identifier of the account.';
COMMENT ON COLUMN group_accounts_view.external_id IS 'External system identifier of the account.';
COMMENT ON COLUMN group_accounts_view.email IS 'Email address of the account.';
COMMENT ON COLUMN group_accounts_view.lastname IS 'Last name of the account holder.';
COMMENT ON COLUMN group_accounts_view.firstname IS 'First name of the account holder.';
COMMENT ON COLUMN group_accounts_view.status IS 'Computed lifecycle status of the account (ACTIVE, SUSPENDED or INACTIVE).';
COMMENT ON COLUMN group_accounts_view.extra_parameters IS 'JSONB column containing custom attributes and metadata associated with the account. Intended for customer-specific or integration-specific extensions that are not part of the standard data model.';
COMMENT ON COLUMN group_accounts_view.relation_extra_parameters IS 'JSONB column containing custom attributes and metadata associated with the relationship between an account and a group. Intended for customer-specific or integration-specific extensions that are not part of the standard data model.';
COMMENT ON COLUMN group_accounts_view.created_by IS 'Full name of the account that attached the account to the group, formatted as "firstname lastname". NULL when the referenced account no longer exists.';
COMMENT ON COLUMN group_accounts_view.updated_by IS 'Full name of the account that last updated the relationship, formatted as "firstname lastname". NULL when the referenced account no longer exists.';
COMMENT ON COLUMN group_accounts_view.insert_date IS 'Date and time when the account was attached to the group. Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN group_accounts_view.update_date IS 'Date and time when the relationship was last updated. Stored in UTC (TIMESTAMPTZ).';
