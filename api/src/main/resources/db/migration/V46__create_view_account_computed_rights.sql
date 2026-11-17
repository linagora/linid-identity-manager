CREATE VIEW account_computed_rights_view AS
SELECT
    accounts.act_id,
    accounts.external_id,
    applications.app_id,
    applications.code AS app_code,
    applications.claims_template,
    account_payloads.payload,
    account_payloads.checksum AS payload_checksum,
    applications.script_checksum,
    applications.claims_template_checksum,
    (account_payloads.act_id IS NOT NULL
        AND (computed_rights.cpr_id IS NULL
            OR computed_rights.payload_checksum IS DISTINCT FROM account_payloads.checksum
            OR computed_rights.script_checksum IS DISTINCT FROM applications.script_checksum
            OR computed_rights.claims_template_checksum IS DISTINCT FROM applications.claims_template_checksum)) AS to_compute
FROM
    accounts
CROSS JOIN applications
LEFT OUTER JOIN account_payloads
    ON account_payloads.act_id = accounts.act_id
LEFT OUTER JOIN computed_rights
    ON computed_rights.act_id = accounts.act_id
    AND computed_rights.app_id = applications.app_id
WHERE
    applications.script IS NOT NULL
    AND applications.deployed_at IS NOT NULL;

COMMENT ON VIEW account_computed_rights_view IS 'Read-only view used internally by sync-api, crossing every account with every application that has a script and a non-null deployed_at, with the payload of the account and the checksums of the pair. Holds one row per (account, application) pair, identified by (act_id, app_id), including the accounts never computed and the applications never computed for an account; empty when no application is deployed.';

COMMENT ON COLUMN account_computed_rights_view.act_id IS 'Identifier of the account. Identifies the row with app_id.';
COMMENT ON COLUMN account_computed_rights_view.external_id IS 'External identifier of the account, used to identify it in the alerts and the logs.';
COMMENT ON COLUMN account_computed_rights_view.app_id IS 'Identifier of the application, which has a script and a non-null deployed_at. Identifies the row with act_id.';
COMMENT ON COLUMN account_computed_rights_view.app_code IS 'Functional unique identifier of the application, used by the OPA call.';
COMMENT ON COLUMN account_computed_rights_view.claims_template IS 'Template used to render the claims of the application from the OPA result.';
COMMENT ON COLUMN account_computed_rights_view.payload IS 'JSONB payload of the account (account_payloads.payload), used as OPA input. NULL when the payload of the account was never built.';
COMMENT ON COLUMN account_computed_rights_view.payload_checksum IS 'Checksum of the payload of the account (account_payloads.checksum), stored with the result of the computation. NULL when the payload of the account was never built.';
COMMENT ON COLUMN account_computed_rights_view.script_checksum IS 'Checksum of the script of the application (applications.script_checksum), stored with the result of the computation.';
COMMENT ON COLUMN account_computed_rights_view.claims_template_checksum IS 'Checksum of the claims template of the application (applications.claims_template_checksum), stored with the result of the computation.';
COMMENT ON COLUMN account_computed_rights_view.to_compute IS 'True when the account has a payload, and the pair has no computed_rights row or its payload, script or claims template checksum differs from the one stored by the last successful computation. False when the account has no payload or when the three checksums are unchanged. A failed computation writes nothing, so the pair stays to compute.';
