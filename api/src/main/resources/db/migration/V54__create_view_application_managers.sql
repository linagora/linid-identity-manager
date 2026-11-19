CREATE OR REPLACE VIEW application_managers_view AS
SELECT
    application_managers.apm_id,
    application_managers.app_id,
    application_managers.act_id,
    accounts.email,
    accounts.lastname,
    accounts.firstname,
    NULLIF(CONCAT_WS(' ', creator.firstname, creator.lastname), '') AS created_by,
    NULLIF(CONCAT_WS(' ', updater.firstname, updater.lastname), '') AS updated_by,
    application_managers.insert_date,
    application_managers.update_date
FROM
    application_managers
JOIN accounts
    ON accounts.act_id = application_managers.act_id
LEFT OUTER JOIN accounts creator
    ON creator.act_id = application_managers.created_by
LEFT OUTER JOIN accounts updater
    ON updater.act_id = application_managers.updated_by;

COMMENT ON VIEW application_managers_view IS 'Provides the managers of each application, enriched with the identity attributes of the manager account and the audit information of the record. Holds one row per (application, account) pair. It reads the accounts table rather than accounts_view, which emits one row per organizational unit of an account.';

COMMENT ON COLUMN application_managers_view.apm_id IS 'Unique identifier of the application manager.';
COMMENT ON COLUMN application_managers_view.app_id IS 'Identifier of the managed application.';
COMMENT ON COLUMN application_managers_view.act_id IS 'Identifier of the manager account.';
COMMENT ON COLUMN application_managers_view.email IS 'Email address of the manager account.';
COMMENT ON COLUMN application_managers_view.lastname IS 'Last name of the manager.';
COMMENT ON COLUMN application_managers_view.firstname IS 'First name of the manager.';
COMMENT ON COLUMN application_managers_view.created_by IS 'Full name of the account that added the manager, formatted as "firstname lastname". NULL when the referenced account no longer exists.';
COMMENT ON COLUMN application_managers_view.updated_by IS 'Full name of the account that last updated the manager record, formatted as "firstname lastname". NULL when the referenced account no longer exists.';
COMMENT ON COLUMN application_managers_view.insert_date IS 'Date and time when the manager was added. Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN application_managers_view.update_date IS 'Date and time when the manager record was last updated. Stored in UTC (TIMESTAMPTZ).';
