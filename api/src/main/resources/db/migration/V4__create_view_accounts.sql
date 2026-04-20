CREATE VIEW accounts_view AS
SELECT a.act_id,
       a.external_id,
       a.email,
       a.lastname,
       a.firstname,
       NULLIF(CONCAT_WS(' ', creator.firstname, creator.lastname), '') AS created_by,
       NULLIF(CONCAT_WS(' ', updater.firstname, updater.lastname), '') AS updated_by,
       a.insert_date,
       a.update_date
FROM accounts a
         LEFT OUTER JOIN accounts creator ON creator.act_id = a.created_by
         LEFT OUTER JOIN accounts updater ON updater.act_id = a.updated_by;

COMMENT ON VIEW accounts_view IS 'Read-only view exposing account records with createdBy and updatedBy resolved to the full name (firstname + lastname) of the referenced account, via LEFT OUTER JOIN on the accounts table.';

COMMENT ON COLUMN accounts_view.act_id IS 'Account unique identifier (UUID).';
COMMENT ON COLUMN accounts_view.external_id IS 'External identifier for the account (e.g. OIDC sub or external system ID).';
COMMENT ON COLUMN accounts_view.email IS 'Email address associated with the account.';
COMMENT ON COLUMN accounts_view.lastname IS 'Last name of the account holder.';
COMMENT ON COLUMN accounts_view.firstname IS 'First name of the account holder.';
COMMENT ON COLUMN accounts_view.created_by IS 'Full name ("firstname lastname") of the account that created this record. Resolved via LEFT OUTER JOIN on accounts.act_id; NULL if the referenced account no longer exists.';
COMMENT ON COLUMN accounts_view.updated_by IS 'Full name ("firstname lastname") of the account that last updated this record. Resolved via LEFT OUTER JOIN on accounts.act_id; NULL if the referenced account no longer exists.';
COMMENT ON COLUMN accounts_view.insert_date IS 'Date and time when the account record was created (UTC).';
COMMENT ON COLUMN accounts_view.update_date IS 'Date and time when the account record was last updated (UTC).';
