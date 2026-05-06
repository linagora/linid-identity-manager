-- All users are created by account admin
WITH generated AS (SELECT gen_random_uuid() AS admin_id),
     accounts_to_insert AS (
         SELECT admin_id AS act_id, 'admin' AS external_id, 'admin@example.com' AS email, 'admin_ln' AS lastname, 'admin_fn' AS firstname, admin_id AS created_by FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'user1', 'user1@example.com', 'user1_ln', 'user1_fn', admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'user2', 'user2@example.com', 'user2_ln', 'user2_fn', admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'user3', 'user3@example.com', 'user3_ln', 'user3_fn', admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'user4', 'user4@example.com', 'user4_ln', 'user4_fn', admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'user5', 'user5@example.com', 'user5_ln', 'user5_fn', admin_id FROM generated
     ),
     inserted_accounts AS (
         INSERT INTO accounts (act_id, external_id, email, lastname, firstname, payload, checksum, created_by, updated_by)
             SELECT act_id,
                    external_id,
                    email,
                    lastname,
                    firstname,
                    '{}'::jsonb,
                    encode(digest('{}', 'sha256'), 'hex'),
                    created_by,
                    created_by
             FROM accounts_to_insert
             ON CONFLICT (email) DO NOTHING
             RETURNING act_id, external_id, created_by
     )
INSERT
INTO account_status (act_id, validity_period, created_by, updated_by)
SELECT act_id,
       tstzrange('2024-01-01 00:00:00+00'::timestamptz, NULL, '[)'),
       created_by,
       created_by
FROM inserted_accounts
-- Exclude admin account from status initialization to allow testing of 404 when no status account row exists yet
WHERE external_id != 'admin';
