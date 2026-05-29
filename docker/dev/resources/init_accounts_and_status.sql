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

-- Each account exposes the minimum required state to trigger the listed dialogs.
-- created_by is self-referencing since no hardcoded admin UUID exists in dev.
INSERT
INTO accounts (act_id, external_id, email, lastname, firstname, payload,
               checksum, created_by, updated_by)
VALUES ('00000000-0000-4000-8000-0000000000d1', 'dialog-d1',
        'dialog-d1@example.com', 'ActivationDialogs', 'Inactive',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-0000000000d1',
        '00000000-0000-4000-8000-0000000000d1'),
       ('00000000-0000-4000-8000-0000000000d2', 'dialog-d2',
        'dialog-d2@example.com', 'SuspDeactDialogs', 'Active',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-0000000000d2',
        '00000000-0000-4000-8000-0000000000d2'),
       ('00000000-0000-4000-8000-0000000000d4', 'dialog-d4',
        'dialog-d4@example.com', 'ReactivImmediate', 'Suspended',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-0000000000d4',
        '00000000-0000-4000-8000-0000000000d4'),
       ('00000000-0000-4000-8000-0000000000d5', 'dialog-d5',
        'dialog-d5@example.com', 'ActScheduledDialog', 'Inactive',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-0000000000d5',
        '00000000-0000-4000-8000-0000000000d5'),
       ('00000000-0000-4000-8000-0000000000d8', 'dialog-d8',
        'dialog-d8@example.com', 'SuspScheduled', 'Active',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-0000000000d8',
        '00000000-0000-4000-8000-0000000000d8'),
       ('00000000-0000-4000-8000-0000000000d9', 'dialog-d9',
        'dialog-d9@example.com', 'ModifySusp', 'Suspended',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-0000000000d9',
        '00000000-0000-4000-8000-0000000000d9')
ON CONFLICT (email) DO NOTHING;

INSERT
INTO account_status (act_id, validity_period, suspension_period, activation_at,
                     status_reason, status_subreason, status_comment,
                     created_by, updated_by)
VALUES
       -- D1: INACTIVE, validity start in the future.
       -- Unlocks: activation.immediate, activation.scheduled, suspension.scheduled, deactivation.scheduled
       ('00000000-0000-4000-8000-0000000000d1',
        tstzrange(now() + interval '30 days', NULL, '[)'),
        NULL,
        NULL,
        'Reason1', 'Subreason1', 'Dialog test D1: activation dialogs',
        '00000000-0000-4000-8000-0000000000d1',
        '00000000-0000-4000-8000-0000000000d1'),
       -- D2: ACTIVE, no end date, no suspension.
       -- Unlocks: suspension.immediate, suspension.scheduled, deactivation.immediate, deactivation.scheduled
       ('00000000-0000-4000-8000-0000000000d2',
        tstzrange(now() - interval '30 days', NULL, '[)'),
        NULL,
        now() - interval '30 days',
        'Reason1', 'Subreason1', 'Dialog test D2: suspension/deactivation immediate dialogs',
        '00000000-0000-4000-8000-0000000000d2',
        '00000000-0000-4000-8000-0000000000d2'),
       -- D4: SUSPENDED, no validity end, no suspension end.
       -- Unlocks: reactivation.immediate
       ('00000000-0000-4000-8000-0000000000d4',
        tstzrange(now() - interval '30 days', NULL, '[)'),
        tstzrange(now() - interval '5 days', NULL, '[)'),
        now() - interval '30 days',
        'Reason2', 'Subreason2', 'Dialog test D4: reactivation.immediate dialog',
        '00000000-0000-4000-8000-0000000000d4',
        '00000000-0000-4000-8000-0000000000d4'),
       -- D5: INACTIVE, validity start in the future.
       -- Unlocks: activation.immediate, activation.scheduled, suspension.scheduled, deactivation.scheduled
       ('00000000-0000-4000-8000-0000000000d5',
        tstzrange(now() + interval '30 days', NULL, '[)'),
        NULL,
        NULL,
        'Reason1', 'Subreason1', 'Dialog test D5: activation.scheduled dialog',
        '00000000-0000-4000-8000-0000000000d5',
        '00000000-0000-4000-8000-0000000000d5'),
       -- D8: ACTIVE, no end date, no suspension.
       -- Unlocks: suspension.scheduled
       ('00000000-0000-4000-8000-0000000000d8',
        tstzrange(now() - interval '30 days', NULL, '[)'),
        NULL,
        now() - interval '30 days',
        'Reason1', 'Subreason1', 'Dialog test D8: suspension.scheduled dialog',
        '00000000-0000-4000-8000-0000000000d8',
        '00000000-0000-4000-8000-0000000000d8'),
       -- D9: SUSPENDED, suspension period with both start and end defined.
       -- Unlocks: suspension.modify (AccountSuspendedBanner)
       ('00000000-0000-4000-8000-0000000000d9',
        tstzrange(now() - interval '30 days', NULL, '[)'),
        tstzrange(now() - interval '10 days', now() + interval '20 days', '[)'),
        now() - interval '30 days',
        'Reason1', 'Subreason1', 'Dialog test D9: modify suspension dialog',
        '00000000-0000-4000-8000-0000000000d9',
        '00000000-0000-4000-8000-0000000000d9')
ON CONFLICT (act_id) DO NOTHING;
