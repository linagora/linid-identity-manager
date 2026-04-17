-- All users are created by account admin
WITH generated AS (SELECT gen_random_uuid() AS admin_id)
INSERT
INTO accounts (act_id,
               external_id,
               email,
               lastname,
               firstname,
               payload,
               checksum,
               created_by,
               updated_by)
SELECT admin_id,
       'admin',
       'admin@example.com',
       'admin_ln',
       'admin_fn',
       '{}'::jsonb,
       encode(digest('{}', 'sha256'), 'hex'),
       admin_id,
       admin_id
FROM generated

UNION ALL

SELECT gen_random_uuid(),
       'user1',
       'user1@example.com',
       'user1_ln',
       'user1_fn',
       '{}'::jsonb,
       encode(digest('{}', 'sha256'), 'hex'),
       admin_id,
       admin_id
FROM generated

UNION ALL

SELECT gen_random_uuid(),
       'user2',
       'user2@example.com',
       'user2_ln',
       'user2_fn',
       '{}'::jsonb,
       encode(digest('{}', 'sha256'), 'hex'),
       admin_id,
       admin_id
FROM generated

ON CONFLICT (email) DO NOTHING;
