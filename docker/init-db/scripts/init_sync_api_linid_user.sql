-- Grants of sync_api_linid_user, the account of sync-api in the linid database.
-- sync-api reads the account data and writes the payloads and the computed
-- rights: it cannot change the other tables, nor create or alter a table.
-- The audit tables are written by SECURITY DEFINER triggers and need no grant.
GRANT USAGE ON SCHEMA public TO sync_api_linid_user;

GRANT SELECT
    ON accounts, applications, application_roles, account_computed_rights_view
    TO sync_api_linid_user;

GRANT SELECT, INSERT, UPDATE
    ON account_payloads, computed_rights
    TO sync_api_linid_user;
