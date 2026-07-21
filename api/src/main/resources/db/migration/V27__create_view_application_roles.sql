CREATE VIEW application_roles_view AS
SELECT r.aro_id,
       r.app_id,
       r.name,
       r.description,
       NULLIF(CONCAT_WS(' ', creator.firstname, creator.lastname), '') AS created_by,
       NULLIF(CONCAT_WS(' ', updater.firstname, updater.lastname), '') AS updated_by,
       r.insert_date,
       r.update_date
FROM application_roles r
         LEFT OUTER JOIN accounts creator ON creator.act_id = r.created_by
         LEFT OUTER JOIN accounts updater ON updater.act_id = r.updated_by;

COMMENT ON VIEW application_roles_view IS 'Read-only view exposing application role records with createdBy/updatedBy resolved to the full name (firstname + lastname) of the referenced account.';

COMMENT ON COLUMN application_roles_view.aro_id IS 'Application role unique identifier (UUID).';
COMMENT ON COLUMN application_roles_view.app_id IS 'Identifier of the application the role belongs to.';
COMMENT ON COLUMN application_roles_view.name IS 'Human-readable name of the role, unique within a given application.';
COMMENT ON COLUMN application_roles_view.description IS 'Optional free-text description of the role.';
COMMENT ON COLUMN application_roles_view.created_by IS 'Full name ("firstname lastname") of the account that created this record. Resolved via LEFT OUTER JOIN on accounts.act_id; NULL if the referenced account no longer exists.';
COMMENT ON COLUMN application_roles_view.updated_by IS 'Full name ("firstname lastname") of the account that last updated this record. Resolved via LEFT OUTER JOIN on accounts.act_id; NULL if the referenced account no longer exists.';
COMMENT ON COLUMN application_roles_view.insert_date IS 'Date and time when the application role record was created (UTC).';
COMMENT ON COLUMN application_roles_view.update_date IS 'Date and time when the application role record was last updated (UTC).';
