CREATE VIEW account_payload_view AS
SELECT
    accounts.act_id,
    COALESCE((SELECT ARRAY_AGG(DISTINCT ou.name ORDER BY ou.name)
              FROM account_organizational_units_view ou
              WHERE ou.act_id = accounts.act_id), '{}') AS organizational_units,
    COALESCE((SELECT ARRAY_AGG(DISTINCT grp.code ORDER BY grp.code)
              FROM account_groups_view grp
              WHERE grp.act_id = accounts.act_id), '{}') AS groups
FROM
    accounts;

COMMENT ON VIEW account_payload_view IS 'Read-only view used internally by sync-api as a source of the account payloads, gathering the multi-valued relations of each account in array columns. Holds one row per account, identified by act_id, with arrays sorted and without duplicates so that the checksum of the payload is stable. The arrays are built by sub-queries, since joining both relations would multiply the rows of each other.';

COMMENT ON COLUMN account_payload_view.act_id IS 'Identifier of the account. Identifies the row.';
COMMENT ON COLUMN account_payload_view.organizational_units IS 'Names of the organizational units the account is attached to, sorted and without duplicates. Empty array when the account has no organizational unit.';
COMMENT ON COLUMN account_payload_view.groups IS 'Codes of the groups the account is attached to, sorted and without duplicates. Empty array when the account has no group.';
