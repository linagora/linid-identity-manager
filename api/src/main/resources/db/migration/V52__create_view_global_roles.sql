CREATE VIEW global_roles_view AS
SELECT
    global_roles.gro_id,
    global_roles.act_id,
    accounts.firstname,
    accounts.lastname,
    accounts.email,
    global_roles.role,
    NULLIF(CONCAT_WS(' ', creator.firstname, creator.lastname), '') AS created_by,
    NULLIF(CONCAT_WS(' ', updater.firstname, updater.lastname), '') AS updated_by,
    global_roles.insert_date,
    global_roles.update_date
FROM
    global_roles
JOIN accounts
    ON accounts.act_id = global_roles.act_id
LEFT OUTER JOIN accounts creator
    ON creator.act_id = global_roles.created_by
LEFT OUTER JOIN accounts updater
    ON updater.act_id = global_roles.updated_by;

COMMENT ON VIEW global_roles_view IS 'Provides the global roles held by accounts, enriched with the identity attributes of the account and the audit information of the assignment. Holds one row per (account, role) pair.';

COMMENT ON COLUMN global_roles_view.gro_id IS 'Unique identifier of the global role assignment.';
COMMENT ON COLUMN global_roles_view.act_id IS 'Identifier of the account holding the role.';
COMMENT ON COLUMN global_roles_view.firstname IS 'First name of the account holder.';
COMMENT ON COLUMN global_roles_view.lastname IS 'Last name of the account holder.';
COMMENT ON COLUMN global_roles_view.email IS 'Email address of the account.';
COMMENT ON COLUMN global_roles_view.role IS 'Global role held by the account.';
COMMENT ON COLUMN global_roles_view.created_by IS 'Full name of the account that assigned the role, formatted as "firstname lastname". NULL when the referenced account no longer exists.';
COMMENT ON COLUMN global_roles_view.updated_by IS 'Full name of the account that last updated the assignment, formatted as "firstname lastname". NULL when the referenced account no longer exists.';
COMMENT ON COLUMN global_roles_view.insert_date IS 'Date and time when the role was assigned. Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN global_roles_view.update_date IS 'Date and time when the assignment was last updated. Stored in UTC (TIMESTAMPTZ).';
