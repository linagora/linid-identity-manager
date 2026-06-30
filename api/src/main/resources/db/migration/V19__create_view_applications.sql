CREATE VIEW applications_view AS
SELECT a.app_id,
       a.code,
       a.name,
       a.description,
       a.type,
       a.claims_template,
       a.script,
       a.script_checksum,
       a.deployed_at,
       a.configuration,
       a.roles,
       NULLIF(CONCAT_WS(' ', creator.firstname, creator.lastname), '') AS created_by,
       NULLIF(CONCAT_WS(' ', updater.firstname, updater.lastname), '') AS updated_by,
       a.insert_date,
       a.update_date
FROM applications a
         LEFT OUTER JOIN accounts creator ON creator.act_id = a.created_by
         LEFT OUTER JOIN accounts updater ON updater.act_id = a.updated_by;

COMMENT ON VIEW applications_view IS 'Read-only view exposing application records with createdBy/updatedBy resolved to the full name (firstname + lastname) of the referenced account.';

COMMENT ON COLUMN applications_view.app_id IS 'Application unique identifier (UUID).';
COMMENT ON COLUMN applications_view.code IS 'Functional unique identifier of the application.';
COMMENT ON COLUMN applications_view.name IS 'Human-readable name of the application.';
COMMENT ON COLUMN applications_view.description IS 'Optional free-text description of the application.';
COMMENT ON COLUMN applications_view.type IS 'Type of the application.';
COMMENT ON COLUMN applications_view.claims_template IS 'Template used to generate the claims exposed to the application.';
COMMENT ON COLUMN applications_view.script IS 'Optional OPA Rego policy script stored to compute the access rights of the application.';
COMMENT ON COLUMN applications_view.script_checksum IS 'Deterministic hash computed from the script. NULL when no script is defined.';
COMMENT ON COLUMN applications_view.deployed_at IS 'Optional date when the application script was deployed on OPA.';
COMMENT ON COLUMN applications_view.configuration IS 'JSONB column storing the application-specific configuration.';
COMMENT ON COLUMN applications_view.roles IS 'JSONB array of strings storing the application roles.';
COMMENT ON COLUMN applications_view.created_by IS 'Full name ("firstname lastname") of the account that created this record. Resolved via LEFT OUTER JOIN on accounts.act_id; NULL if the referenced account no longer exists.';
COMMENT ON COLUMN applications_view.updated_by IS 'Full name ("firstname lastname") of the account that last updated this record. Resolved via LEFT OUTER JOIN on accounts.act_id; NULL if the referenced account no longer exists.';
COMMENT ON COLUMN applications_view.insert_date IS 'Date and time when the application record was created (UTC).';
COMMENT ON COLUMN applications_view.update_date IS 'Date and time when the application record was last updated (UTC).';
