CREATE VIEW account_organizational_units_view AS
SELECT DISTINCT
    organizational_unit_accounts.act_id,
    organizational_units_view.oun_id,
    organizational_units_view.name,
    organizational_units_view.type,
    organizational_units_view.status,
    organizational_unit_accounts.extra_parameters AS "relation_extra_parameters"
FROM
    organizational_unit_accounts
JOIN organizational_units_view
    ON organizational_units_view.oun_id = organizational_unit_accounts.oun_id;

COMMENT ON VIEW account_organizational_units_view IS 'Provides the organizational units each account is attached to, enriched with the organizational unit name, type and computed status. Holds one row per (account, organizational unit) pair; accounts without any membership are absent. DISTINCT collapses the rows organizational_units_view emits for each parent of a multi-parent organizational unit.';

COMMENT ON COLUMN account_organizational_units_view.act_id IS 'Identifier of the account attached to the organizational unit.';
COMMENT ON COLUMN account_organizational_units_view.oun_id IS 'Unique identifier of the organizational unit.';
COMMENT ON COLUMN account_organizational_units_view.name IS 'Human-readable name of the organizational unit.';
COMMENT ON COLUMN account_organizational_units_view.type IS 'Type of the organizational unit.';
COMMENT ON COLUMN account_organizational_units_view.status IS 'Computed lifecycle status of the organizational unit (ACTIVE or SUSPENDED).';
COMMENT ON COLUMN account_organizational_units_view.relation_extra_parameters IS 'JSONB column containing custom attributes and metadata associated with the relationship between an account and an organizational unit. Intended for customer-specific or integration-specific extensions that are not part of the standard data model.';
