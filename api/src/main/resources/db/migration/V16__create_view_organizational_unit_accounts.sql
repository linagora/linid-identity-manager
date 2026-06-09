CREATE VIEW organizational_unit_accounts_view AS
SELECT DISTINCT
    accounts_view.act_id,
    accounts_view.external_id,
    accounts_view.email,
    accounts_view.lastname,
    accounts_view.firstname,
    accounts_view.validity_period,
    accounts_view.suspension_period,
    accounts_view.activation_at,
    accounts_view.suspension_reason,
    accounts_view.suspension_subreason,
    accounts_view.suspension_comment,
    accounts_view.deactivation_reason,
    accounts_view.deactivation_subreason,
    accounts_view.deactivation_comment,
    accounts_view.reactivation_comment,
    accounts_view.status,
    accounts_view.days_before_deactivation,
    accounts_view.created_by,
    accounts_view.updated_by,
    accounts_view.insert_date,
    accounts_view.update_date,
    organizational_unit_accounts.oun_id
FROM
    accounts_view
LEFT OUTER JOIN organizational_unit_accounts
    ON organizational_unit_accounts.act_id = accounts_view.act_id;

COMMENT ON VIEW organizational_unit_accounts_view IS 'Provides a denormalized view of accounts enriched with their associated organizational unit identifiers.';

COMMENT ON COLUMN organizational_unit_accounts_view.act_id IS 'Unique identifier of the account.';
COMMENT ON COLUMN organizational_unit_accounts_view.external_id IS 'External system identifier of the account.';
COMMENT ON COLUMN organizational_unit_accounts_view.email IS 'Email address of the account.';
COMMENT ON COLUMN organizational_unit_accounts_view.lastname IS 'Last name of the account holder.';
COMMENT ON COLUMN organizational_unit_accounts_view.firstname IS 'First name of the account holder.';
COMMENT ON COLUMN organizational_unit_accounts_view.validity_period IS 'Time period during which the account is considered valid.';
COMMENT ON COLUMN organizational_unit_accounts_view.suspension_period IS 'Time period during which the account is suspended.';
COMMENT ON COLUMN organizational_unit_accounts_view.activation_at IS 'Timestamp when the account was activated.';
COMMENT ON COLUMN organizational_unit_accounts_view.suspension_reason IS 'High-level reason code explaining the suspension (inherited from accounts_view).';
COMMENT ON COLUMN organizational_unit_accounts_view.suspension_subreason IS 'More detailed classification of the suspension reason (inherited from accounts_view).';
COMMENT ON COLUMN organizational_unit_accounts_view.suspension_comment IS 'Free-text comment about the suspension (inherited from accounts_view).';
COMMENT ON COLUMN organizational_unit_accounts_view.deactivation_reason IS 'High-level reason code explaining the deactivation (inherited from accounts_view).';
COMMENT ON COLUMN organizational_unit_accounts_view.deactivation_subreason IS 'More detailed classification of the deactivation reason (inherited from accounts_view).';
COMMENT ON COLUMN organizational_unit_accounts_view.deactivation_comment IS 'Free-text comment about the deactivation (inherited from accounts_view).';
COMMENT ON COLUMN organizational_unit_accounts_view.reactivation_comment IS 'Free-text comment about a reactivation (inherited from accounts_view).';
COMMENT ON COLUMN organizational_unit_accounts_view.status IS 'Current status of the account.';
COMMENT ON COLUMN organizational_unit_accounts_view.days_before_deactivation IS 'Number of days before the account is automatically deactivated.';
COMMENT ON COLUMN organizational_unit_accounts_view.created_by IS 'Identifier of the creator of the account record.';
COMMENT ON COLUMN organizational_unit_accounts_view.updated_by IS 'Identifier of the last updater of the account record.';
COMMENT ON COLUMN organizational_unit_accounts_view.insert_date IS 'Timestamp when the account record was created (UTC).';
COMMENT ON COLUMN organizational_unit_accounts_view.update_date IS 'Timestamp when the account record was last updated (UTC).';
COMMENT ON COLUMN organizational_unit_accounts_view.oun_id IS 'Identifier of the organizational unit associated with the account.';
