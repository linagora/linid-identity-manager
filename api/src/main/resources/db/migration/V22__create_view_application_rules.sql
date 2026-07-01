CREATE VIEW application_rules_view AS
SELECT r.aru_id,
       r.app_id,
       r.code,
       r.description,
       r.priority,
       r.script,
       r.script_checksum,
       r.disabled,
       NULLIF(CONCAT_WS(' ', creator.firstname, creator.lastname), '') AS created_by,
       NULLIF(CONCAT_WS(' ', updater.firstname, updater.lastname), '') AS updated_by,
       r.insert_date,
       r.update_date
FROM application_rules r
         LEFT OUTER JOIN accounts creator ON creator.act_id = r.created_by
         LEFT OUTER JOIN accounts updater ON updater.act_id = r.updated_by;

COMMENT ON VIEW application_rules_view IS 'Read-only view exposing application rule records with createdBy/updatedBy resolved to the full name (firstname + lastname) of the referenced account.';

COMMENT ON COLUMN application_rules_view.aru_id IS 'Application rule unique identifier (UUID).';
COMMENT ON COLUMN application_rules_view.app_id IS 'Identifier of the application the rule belongs to.';
COMMENT ON COLUMN application_rules_view.code IS 'Functional identifier of the rule, unique within a given application.';
COMMENT ON COLUMN application_rules_view.description IS 'Optional free-text description of the rule.';
COMMENT ON COLUMN application_rules_view.priority IS 'Execution priority of the rule. Lower values are executed first.';
COMMENT ON COLUMN application_rules_view.script IS 'OPA Rego policy script computing the access rights granted by the rule.';
COMMENT ON COLUMN application_rules_view.script_checksum IS 'Deterministic hash (SHA-256) computed from the script.';
COMMENT ON COLUMN application_rules_view.disabled IS 'Whether the rule is disabled.';
COMMENT ON COLUMN application_rules_view.created_by IS 'Full name ("firstname lastname") of the account that created this record. Resolved via LEFT OUTER JOIN on accounts.act_id; NULL if the referenced account no longer exists.';
COMMENT ON COLUMN application_rules_view.updated_by IS 'Full name ("firstname lastname") of the account that last updated this record. Resolved via LEFT OUTER JOIN on accounts.act_id; NULL if the referenced account no longer exists.';
COMMENT ON COLUMN application_rules_view.insert_date IS 'Date and time when the application rule record was created (UTC).';
COMMENT ON COLUMN application_rules_view.update_date IS 'Date and time when the application rule record was last updated (UTC).';
