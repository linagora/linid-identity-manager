CREATE VIEW accounts_view AS
SELECT a.act_id,
       a.external_id,
       a.email,
       a.lastname,
       a.firstname,
       s.validity_period,
       s.suspension_period,
       s.activation_at,
       s.status_reason,
       s.status_subreason,
       s.status_comment,
       CASE
           WHEN s.activation_at IS NOT NULL
                AND lower(s.validity_period) IS NOT NULL
                AND now() >= lower(s.validity_period)
                AND (upper(s.validity_period) IS NULL OR now() <= upper(s.validity_period))
                AND s.suspension_period IS NOT NULL
                AND lower(s.suspension_period) IS NOT NULL
                AND now() >= lower(s.suspension_period)
                AND (upper(s.suspension_period) IS NULL OR now() <= upper(s.suspension_period))
               THEN 'SUSPENDED'
           WHEN s.activation_at IS NOT NULL
                AND lower(s.validity_period) IS NOT NULL
                AND now() >= lower(s.validity_period)
                AND (upper(s.validity_period) IS NULL OR now() <= upper(s.validity_period))
                AND (
                    s.suspension_period IS NULL
                        OR lower(s.suspension_period) IS NULL
                        OR now() < lower(s.suspension_period)
                        OR (
                            upper(s.suspension_period) IS NOT NULL
                                AND now() > upper(s.suspension_period)
                            )
                    )
               THEN 'ACTIVE'
           ELSE 'INACTIVE'
           END AS status,
       CASE
           WHEN upper(s.validity_period) IS NOT NULL
               THEN (DATE(upper(s.validity_period)) - CURRENT_DATE)::INTEGER
           ELSE NULL
           END AS days_before_deactivation,
       NULLIF(CONCAT_WS(' ', creator.firstname, creator.lastname), '') AS created_by,
       NULLIF(CONCAT_WS(' ', updater.firstname, updater.lastname), '') AS updated_by,
       a.insert_date,
       a.update_date
FROM accounts a
         LEFT OUTER JOIN account_status s ON s.act_id = a.act_id
         LEFT OUTER JOIN accounts creator ON creator.act_id = a.created_by
         LEFT OUTER JOIN accounts updater ON updater.act_id = a.updated_by;

COMMENT ON VIEW accounts_view IS 'Read-only view exposing account records enriched with status lifecycle information (validity and suspension periods, activation timestamp, reasons and comment), a computed status (ACTIVE / SUSPENDED / INACTIVE), a computed days_before_deactivation integer, and createdBy/updatedBy resolved to the full name (firstname + lastname) of the referenced account.';

COMMENT ON COLUMN accounts_view.act_id IS 'Account unique identifier (UUID).';
COMMENT ON COLUMN accounts_view.external_id IS 'External identifier for the account (e.g. OIDC sub or external system ID).';
COMMENT ON COLUMN accounts_view.email IS 'Email address associated with the account.';
COMMENT ON COLUMN accounts_view.lastname IS 'Last name of the account holder.';
COMMENT ON COLUMN accounts_view.firstname IS 'First name of the account holder.';
COMMENT ON COLUMN accounts_view.validity_period IS 'Time range during which the account is considered valid. NULL when no status row exists for the account.';
COMMENT ON COLUMN accounts_view.suspension_period IS 'Time range during which the account is suspended. NULL when no suspension is configured. An open-ended suspension (NULL upper bound) is treated as a permanent suspension.';
COMMENT ON COLUMN accounts_view.activation_at IS 'Timestamp when the account was activated or reactivated. NULL until the account is activated.';
COMMENT ON COLUMN accounts_view.status_reason IS 'High-level reason code explaining the current status.';
COMMENT ON COLUMN accounts_view.status_subreason IS 'More detailed classification of the status reason.';
COMMENT ON COLUMN accounts_view.status_comment IS 'Free-text comment providing additional context about the status change.';
COMMENT ON COLUMN accounts_view.status IS 'Computed account status: ACTIVE when activated and inside the validity period outside any suspension; SUSPENDED when activated, inside validity, and inside a suspension period (including suspensions with no upper bound); INACTIVE otherwise.';
COMMENT ON COLUMN accounts_view.days_before_deactivation IS 'Integer number of calendar days between today and the upper bound of the validity period. Can be negative. NULL when the validity period has no upper bound.';
COMMENT ON COLUMN accounts_view.created_by IS 'Full name ("firstname lastname") of the account that created this record. Resolved via LEFT OUTER JOIN on accounts.act_id; NULL if the referenced account no longer exists.';
COMMENT ON COLUMN accounts_view.updated_by IS 'Full name ("firstname lastname") of the account that last updated this record. Resolved via LEFT OUTER JOIN on accounts.act_id; NULL if the referenced account no longer exists.';
COMMENT ON COLUMN accounts_view.insert_date IS 'Date and time when the account record was created (UTC).';
COMMENT ON COLUMN accounts_view.update_date IS 'Date and time when the account record was last updated (UTC).';
