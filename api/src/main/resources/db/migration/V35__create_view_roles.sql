CREATE VIEW roles_view AS
SELECT r.rol_id,
       r.code,
       r.name,
       r.description,
       oua.oun_id AS organizational_unit_id,
       ou_list.organizational_units,
       r.extra_parameters,
       NULLIF(CONCAT_WS(' ', creator.firstname, creator.lastname), '') AS created_by,
       NULLIF(CONCAT_WS(' ', updater.firstname, updater.lastname), '') AS updated_by,
       r.insert_date,
       r.update_date
FROM roles r

    LEFT OUTER JOIN accounts creator
        ON creator.act_id = r.created_by

    LEFT OUTER JOIN accounts updater
        ON updater.act_id = r.updated_by

    LEFT OUTER JOIN (
        SELECT DISTINCT rol_id, oun_id
        FROM organizational_unit_accounts
    ) oua
        ON oua.rol_id = r.rol_id

    LEFT OUTER JOIN (
        SELECT oua.rol_id,
               STRING_AGG(ou.name, ', ' ORDER BY ou.name) AS organizational_units
        FROM (
            SELECT DISTINCT rol_id, oun_id
            FROM organizational_unit_accounts
        ) oua
            JOIN organizational_units ou
                ON ou.oun_id = oua.oun_id
        GROUP BY oua.rol_id
    ) ou_list
        ON ou_list.rol_id = r.rol_id;

COMMENT ON VIEW roles_view IS 'Read-only view exposing role records with created_by and updated_by resolved to human-readable account names, together with the organizational units in which the role is held through organizational_unit_accounts. One row is returned for each organizational unit in which the role is held; roles without any association are returned once. Intended for display, reporting, and API consumption.';

COMMENT ON COLUMN roles_view.rol_id IS 'Unique identifier of the role (UUID).';
COMMENT ON COLUMN roles_view.code IS 'Unique technical identifier of the role.';
COMMENT ON COLUMN roles_view.name IS 'Human-readable name of the role.';
COMMENT ON COLUMN roles_view.description IS 'Optional description providing additional details about the role.';
COMMENT ON COLUMN roles_view.organizational_unit_id IS 'Identifier of an organizational unit in which the role is held by at least one account. One row is returned for each such organizational unit. NULL when the role is not held anywhere.';
COMMENT ON COLUMN roles_view.organizational_units IS 'Comma-separated names of all organizational units in which the role is held. NULL when the role is not held anywhere.';
COMMENT ON COLUMN roles_view.extra_parameters IS 'JSONB column containing custom attributes and metadata associated with the role.';
COMMENT ON COLUMN roles_view.created_by IS 'Full name of the account that created this role, formatted as "firstname lastname". Resolved through a LEFT JOIN on the accounts table. NULL when the referenced account no longer exists.';
COMMENT ON COLUMN roles_view.updated_by IS 'Full name of the account that last updated this role, formatted as "firstname lastname". Resolved through a LEFT JOIN on the accounts table. NULL when the referenced account no longer exists.';
COMMENT ON COLUMN roles_view.insert_date IS 'Date and time when the role was created. Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN roles_view.update_date IS 'Date and time when the role was last updated. Stored in UTC (TIMESTAMPTZ).';
