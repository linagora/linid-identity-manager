CREATE VIEW account_organizational_units_view AS
SELECT DISTINCT
    organizational_unit_accounts.act_id,
    organizational_units_view.oun_id,
    organizational_units_view.name,
    organizational_units_view.type,
    organizational_units_view.status,
    organizational_unit_accounts.extra_parameters AS "relation_extra_parameters",
    NULLIF(CONCAT_WS(' ', creator.firstname, creator.lastname), '') AS created_by,
    NULLIF(CONCAT_WS(' ', updater.firstname, updater.lastname), '') AS updated_by,
    organizational_unit_accounts.insert_date,
    organizational_unit_accounts.update_date,
    organizational_unit_accounts.rol_id AS "role_id",
    roles.name AS "role_name"
FROM
    organizational_unit_accounts
JOIN organizational_units_view
    ON organizational_units_view.oun_id = organizational_unit_accounts.oun_id
LEFT OUTER JOIN roles
    ON roles.rol_id = organizational_unit_accounts.rol_id
LEFT OUTER JOIN accounts creator
    ON creator.act_id = organizational_unit_accounts.created_by
LEFT OUTER JOIN accounts updater
    ON updater.act_id = organizational_unit_accounts.updated_by;

COMMENT ON VIEW account_organizational_units_view IS 'Provides the organizational units each account is attached to, enriched with the organizational unit name, type and computed status, with the functional role held by the account within the organizational unit, and with the audit information of the relationship. Holds one row per (account, organizational unit) pair; accounts without any membership are absent. DISTINCT collapses the rows organizational_units_view emits for each parent of a multi-parent organizational unit.';

COMMENT ON COLUMN account_organizational_units_view.act_id IS 'Identifier of the account attached to the organizational unit.';
COMMENT ON COLUMN account_organizational_units_view.oun_id IS 'Unique identifier of the organizational unit.';
COMMENT ON COLUMN account_organizational_units_view.name IS 'Human-readable name of the organizational unit.';
COMMENT ON COLUMN account_organizational_units_view.type IS 'Type of the organizational unit.';
COMMENT ON COLUMN account_organizational_units_view.status IS 'Computed lifecycle status of the organizational unit (ACTIVE or SUSPENDED).';
COMMENT ON COLUMN account_organizational_units_view.created_by IS 'Full name of the account that attached the account to the organizational unit, formatted as "firstname lastname". NULL when the referenced account no longer exists.';
COMMENT ON COLUMN account_organizational_units_view.updated_by IS 'Full name of the account that last updated the relationship, formatted as "firstname lastname". NULL when the referenced account no longer exists.';
COMMENT ON COLUMN account_organizational_units_view.insert_date IS 'Date and time when the account was attached to the organizational unit. Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN account_organizational_units_view.update_date IS 'Date and time when the relationship was last updated. Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN account_organizational_units_view.relation_extra_parameters IS 'JSONB column containing custom attributes and metadata associated with the relationship between an account and an organizational unit. Intended for customer-specific or integration-specific extensions that are not part of the standard data model.';
COMMENT ON COLUMN account_organizational_units_view.role_id IS 'Identifier of the functional role held by the account within the organizational unit. NULL when the role has been deleted.';
COMMENT ON COLUMN account_organizational_units_view.role_name IS 'Human-readable name of the functional role held by the account within the organizational unit. NULL when the role has been deleted.';
