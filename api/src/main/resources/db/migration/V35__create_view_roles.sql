CREATE VIEW roles_view AS
SELECT r.rol_id,
       r.code,
       r.name,
       r.description,
       r.extra_parameters,
       NULLIF(CONCAT_WS(' ', creator.firstname, creator.lastname), '') AS created_by,
       NULLIF(CONCAT_WS(' ', updater.firstname, updater.lastname), '') AS updated_by,
       r.insert_date,
       r.update_date
FROM roles r
         LEFT OUTER JOIN accounts creator ON creator.act_id = r.created_by
         LEFT OUTER JOIN accounts updater ON updater.act_id = r.updated_by;

COMMENT ON VIEW roles_view IS 'Read-only view exposing role records with created_by and updated_by resolved to human-readable account names. Intended for display, reporting, and API consumption.';

COMMENT ON COLUMN roles_view.rol_id IS 'Unique identifier of the role (UUID).';
COMMENT ON COLUMN roles_view.code IS 'Unique technical identifier of the role.';
COMMENT ON COLUMN roles_view.name IS 'Human-readable name of the role.';
COMMENT ON COLUMN roles_view.description IS 'Optional description providing additional details about the role.';
COMMENT ON COLUMN roles_view.extra_parameters IS 'JSONB column containing custom attributes and metadata associated with the role.';
COMMENT ON COLUMN roles_view.created_by IS 'Full name of the account that created this role, formatted as "firstname lastname". Resolved through a LEFT JOIN on the accounts table. NULL when the referenced account no longer exists.';
COMMENT ON COLUMN roles_view.updated_by IS 'Full name of the account that last updated this role, formatted as "firstname lastname". Resolved through a LEFT JOIN on the accounts table. NULL when the referenced account no longer exists.';
COMMENT ON COLUMN roles_view.insert_date IS 'Date and time when the role was created. Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN roles_view.update_date IS 'Date and time when the role was last updated. Stored in UTC (TIMESTAMPTZ).';
