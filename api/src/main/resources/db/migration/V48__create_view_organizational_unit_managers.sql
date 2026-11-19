CREATE VIEW organizational_unit_managers_view AS
SELECT
    organizational_unit_managers.oum_id,
    organizational_unit_managers.oun_id,
    organizational_unit_managers.act_id,
    accounts.email,
    accounts.lastname,
    accounts.firstname,
    NULLIF(CONCAT_WS(' ', creator.firstname, creator.lastname), '') AS created_by,
    NULLIF(CONCAT_WS(' ', updater.firstname, updater.lastname), '') AS updated_by,
    organizational_unit_managers.insert_date,
    organizational_unit_managers.update_date
FROM
    organizational_unit_managers
JOIN accounts
    ON accounts.act_id = organizational_unit_managers.act_id
LEFT OUTER JOIN accounts creator
    ON creator.act_id = organizational_unit_managers.created_by
LEFT OUTER JOIN accounts updater
    ON updater.act_id = organizational_unit_managers.updated_by;

COMMENT ON VIEW organizational_unit_managers_view IS 'Provides the managers of each organizational unit, enriched with the identity attributes of the manager account and the audit information of the record. Holds one row per (organizational unit, account) pair. It reads the accounts table rather than accounts_view, which emits one row per organizational unit of an account.';

COMMENT ON COLUMN organizational_unit_managers_view.oum_id IS 'Unique identifier of the organizational unit manager.';
COMMENT ON COLUMN organizational_unit_managers_view.oun_id IS 'Identifier of the managed organizational unit.';
COMMENT ON COLUMN organizational_unit_managers_view.act_id IS 'Identifier of the manager account.';
COMMENT ON COLUMN organizational_unit_managers_view.email IS 'Email address of the manager account.';
COMMENT ON COLUMN organizational_unit_managers_view.lastname IS 'Last name of the manager.';
COMMENT ON COLUMN organizational_unit_managers_view.firstname IS 'First name of the manager.';
COMMENT ON COLUMN organizational_unit_managers_view.created_by IS 'Full name of the account that added the manager, formatted as "firstname lastname". NULL when the referenced account no longer exists.';
COMMENT ON COLUMN organizational_unit_managers_view.updated_by IS 'Full name of the account that last updated the manager record, formatted as "firstname lastname". NULL when the referenced account no longer exists.';
COMMENT ON COLUMN organizational_unit_managers_view.insert_date IS 'Date and time when the manager was added. Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN organizational_unit_managers_view.update_date IS 'Date and time when the manager record was last updated. Stored in UTC (TIMESTAMPTZ).';
