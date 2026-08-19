CREATE OR REPLACE VIEW accounts_view AS
SELECT a.act_id,
       a.external_id,
       a.email,
       a.lastname,
       a.firstname,
       oua.oun_id AS organizational_unit_id,
       ou_list.organizational_units,
       a.extra_parameters,
       s.validity_period,
       s.suspension_period,
       s.activation_at,
       s.suspension_reason,
       s.suspension_subreason,
       s.suspension_comment,
       s.deactivation_reason,
       s.deactivation_subreason,
       s.deactivation_comment,
       s.reactivation_comment,
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
           END AS days_before_deactivation,
       NULLIF(CONCAT_WS(' ', creator.firstname, creator.lastname), '') AS created_by,
       NULLIF(CONCAT_WS(' ', updater.firstname, updater.lastname), '') AS updated_by,
       a.insert_date,
       a.update_date
FROM accounts a
    LEFT JOIN account_status s
           ON s.act_id = a.act_id

    LEFT JOIN accounts creator
           ON creator.act_id = a.created_by

    LEFT JOIN accounts updater
           ON updater.act_id = a.updated_by

    LEFT JOIN organizational_unit_accounts oua
           ON oua.act_id = a.act_id

    LEFT JOIN (
        SELECT oua.act_id,
               STRING_AGG(ou.name, ', ' ORDER BY ou.name) AS organizational_units
        FROM organizational_unit_accounts oua
                 JOIN organizational_units ou
                      ON ou.oun_id = oua.oun_id
        GROUP BY oua.act_id
    ) ou_list
    ON ou_list.act_id = a.act_id;

COMMENT ON VIEW accounts_view IS 'View exposing account information together with organizational unit associations, account lifecycle status, suspension and deactivation details, and audit information.';

COMMENT ON COLUMN accounts_view.act_id IS 'Primary key (UUID) of the account.';
COMMENT ON COLUMN accounts_view.external_id IS 'External identifier for the account, such as an OIDC sub or external system ID.';
COMMENT ON COLUMN accounts_view.email IS 'Email address associated with the account.';
COMMENT ON COLUMN accounts_view.lastname IS 'Last name of the account holder.';
COMMENT ON COLUMN accounts_view.firstname IS 'First name of the account holder.';
COMMENT ON COLUMN accounts_view.organizational_unit_id IS 'Identifier of the organizational unit associated with the account. One row is returned for each organizational unit association.';
COMMENT ON COLUMN accounts_view.organizational_units IS 'Comma-separated names of all organizational units associated with the account.';
COMMENT ON COLUMN accounts_view.extra_parameters IS 'JSONB column containing custom attributes and metadata defined by the deployment. Intended for customer-specific or integration-specific extensions that are not part of the standard data model.';
COMMENT ON COLUMN accounts_view.validity_period IS 'Time range during which the account is valid. NULL when no validity period is configured.';
COMMENT ON COLUMN accounts_view.suspension_period IS 'Time range during which the account is suspended. NULL when no suspension is configured. An open-ended suspension (NULL upper bound) is treated as a permanent suspension.';
COMMENT ON COLUMN accounts_view.activation_at IS 'Date and time when the account was activated.';
COMMENT ON COLUMN accounts_view.suspension_reason IS 'High-level reason code explaining the account suspension.';
COMMENT ON COLUMN accounts_view.suspension_subreason IS 'More detailed classification of the account suspension reason.';
COMMENT ON COLUMN accounts_view.suspension_comment IS 'Free-text comment providing additional context about the account suspension.';
COMMENT ON COLUMN accounts_view.deactivation_reason IS 'High-level reason code explaining the account deactivation.';
COMMENT ON COLUMN accounts_view.deactivation_subreason IS 'More detailed classification of the account deactivation reason.';
COMMENT ON COLUMN accounts_view.deactivation_comment IS 'Free-text comment providing additional context about the account deactivation.';
COMMENT ON COLUMN accounts_view.reactivation_comment IS 'Free-text comment providing additional context about the account reactivation.';
COMMENT ON COLUMN accounts_view.status IS 'Computed lifecycle status of the account. SUSPENDED when the account is currently suspended, ACTIVE when the account is currently valid and not suspended, and INACTIVE otherwise.';
COMMENT ON COLUMN accounts_view.days_before_deactivation IS 'Number of days remaining before the end of the account validity period. NULL when the validity period has no upper bound.';
COMMENT ON COLUMN accounts_view.created_by IS 'Display name of the user, service, or system that created the account.';
COMMENT ON COLUMN accounts_view.updated_by IS 'Display name of the user, service, or system that last updated the account.';
COMMENT ON COLUMN accounts_view.insert_date IS 'Date and time when the account record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN accounts_view.update_date IS 'Date and time when the account record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';
