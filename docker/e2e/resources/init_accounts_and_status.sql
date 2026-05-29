-- All users are created by account admin. The admin row uses a deterministic
-- UUID so other rows can reference it directly through created_by / updated_by.
INSERT
INTO accounts (act_id, external_id, email, lastname, firstname, payload,
               checksum, created_by, updated_by)
VALUES ('00000000-0000-0000-0000-00000000a001', 'admin',
        'admin@example.com', 'admin_ln', 'admin_fn',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       ('00000000-0000-0000-0000-00000000a002', 'user1', 'user1@example.com', 'user1_ln', 'user1_fn',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       ('00000000-0000-0000-0000-00000000a003', 'user2', 'user2@example.com', 'user2_ln', 'user2_fn',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       ('00000000-0000-0000-0000-00000000a004', 'user3', 'user3@example.com', 'user3_ln', 'user3_fn',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       ('00000000-0000-0000-0000-00000000a005', 'user4', 'user4@example.com', 'user4_ln', 'user4_fn',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       ('00000000-0000-0000-0000-00000000a006', 'user5', 'user5@example.com', 'user5_ln', 'user5_fn',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001')
ON CONFLICT (email) DO NOTHING;

-- Lifecycle test accounts. Each row covers one case of the lifecycle UI
-- matrix from issue #112. UUIDs are deterministic so that e2e scenarios can
-- target them directly through /accounts/{id}. account_status rows below are
-- expressed relatively to now() so the cases stay valid regardless of when
-- the test environment is rebuilt.
INSERT
INTO accounts (act_id, external_id, email, lastname, firstname, payload,
               checksum, created_by, updated_by)
VALUES ('00000000-0000-0000-0000-0000000000c1', 'lifecycle-c1',
        'lifecycle-c1@example.com', 'FutureStart', 'Inactive',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       ('00000000-0000-0000-0000-0000000000c2', 'lifecycle-c2',
        'lifecycle-c2@example.com', 'NotActivated', 'Inactive',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       ('00000000-0000-0000-0000-0000000000c3', 'lifecycle-c3',
        'lifecycle-c3@example.com', 'NoEnd', 'Active',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       ('00000000-0000-0000-0000-0000000000c4', 'lifecycle-c4',
        'lifecycle-c4@example.com', 'EndFar', 'Active',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       ('00000000-0000-0000-0000-0000000000c5', 'lifecycle-c5',
        'lifecycle-c5@example.com', 'EndSoon', 'Active',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       ('00000000-0000-0000-0000-0000000000c6', 'lifecycle-c6',
        'lifecycle-c6@example.com', 'NoEndSuspending', 'Active',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       ('00000000-0000-0000-0000-0000000000c7', 'lifecycle-c7',
        'lifecycle-c7@example.com', 'EndFarSuspending', 'Active',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       ('00000000-0000-0000-0000-0000000000c8', 'lifecycle-c8',
        'lifecycle-c8@example.com', 'EndSoonSuspending', 'Active',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       ('00000000-0000-0000-0000-0000000000c9', 'lifecycle-c9',
        'lifecycle-c9@example.com', 'NoEnd', 'Suspended',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       ('00000000-0000-0000-0000-00000000000a', 'lifecycle-c10',
        'lifecycle-c10@example.com', 'WithSuspensionEnd', 'Suspended',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       ('00000000-0000-0000-0000-00000000000b', 'lifecycle-c11',
        'lifecycle-c11@example.com', 'EndFar', 'Suspended',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       ('00000000-0000-0000-0000-00000000000c', 'lifecycle-c12',
        'lifecycle-c12@example.com', 'EndSoon', 'Suspended',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001')
ON CONFLICT (email) DO NOTHING;

-- Account status for basic users (user1-5 only).
-- Admin is excluded to allow testing of 404 when no status account row exists yet.
INSERT
INTO account_status (act_id, validity_period, created_by, updated_by)
SELECT a.act_id,
       tstzrange('2024-01-01 00:00:00+00'::timestamptz, NULL, '[)'),
       '00000000-0000-0000-0000-00000000a001',
       '00000000-0000-0000-0000-00000000a001'
FROM accounts a
WHERE a.external_id IN ('user1', 'user2', 'user3', 'user4', 'user5')
ON CONFLICT (act_id) DO NOTHING;

INSERT
INTO account_status (act_id, validity_period, suspension_period, activation_at,
                     status_reason, status_subreason, status_comment,
                     created_by, updated_by)
VALUES
       -- Case 1: INACTIVE, validity start in the future, never activated.
       ('00000000-0000-0000-0000-0000000000c1',
        tstzrange(now() + interval '30 days', NULL, '[)'),
        NULL,
        NULL,
        'ONBOARDING', NULL, 'E2E case 1',
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       -- Case 2: INACTIVE, validity already started but never activated.
       ('00000000-0000-0000-0000-0000000000c2',
        tstzrange(now() - interval '30 days', NULL, '[)'),
        NULL,
        NULL,
        'ONBOARDING', NULL, 'E2E case 2',
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       -- Case 3: ACTIVE, no end date, no suspension.
       ('00000000-0000-0000-0000-0000000000c3',
        tstzrange(now() - interval '30 days', NULL, '[)'),
        NULL,
        now() - interval '30 days',
        'ONBOARDING', NULL, 'E2E case 3',
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       -- Case 4: ACTIVE, end > 15 days, no suspension.
       ('00000000-0000-0000-0000-0000000000c4',
        tstzrange(now() - interval '30 days', now() + interval '30 days', '[)'),
        NULL,
        now() - interval '30 days',
        'ONBOARDING', NULL, 'E2E case 4',
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       -- Case 5: ACTIVE, end <= 15 days, no suspension.
       ('00000000-0000-0000-0000-0000000000c5',
        tstzrange(now() - interval '30 days', now() + interval '10 days', '[)'),
        NULL,
        now() - interval '30 days',
        'ONBOARDING', NULL, 'E2E case 5',
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       -- Case 6: ACTIVE, no end date, future suspension planned.
       ('00000000-0000-0000-0000-0000000000c6',
        tstzrange(now() - interval '30 days', NULL, '[)'),
        tstzrange(now() + interval '10 days', now() + interval '20 days', '[)'),
        now() - interval '30 days',
        'ONBOARDING', NULL, 'E2E case 6',
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       -- Case 7: ACTIVE, end > 15 days, future suspension planned.
       ('00000000-0000-0000-0000-0000000000c7',
        tstzrange(now() - interval '30 days', now() + interval '30 days', '[)'),
        tstzrange(now() + interval '10 days', now() + interval '20 days', '[)'),
        now() - interval '30 days',
        'ONBOARDING', NULL, 'E2E case 7',
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       -- Case 8: ACTIVE, end <= 15 days, future suspension planned.
       ('00000000-0000-0000-0000-0000000000c8',
        tstzrange(now() - interval '30 days', now() + interval '10 days', '[)'),
        tstzrange(now() + interval '5 days', now() + interval '8 days', '[)'),
        now() - interval '30 days',
        'ONBOARDING', NULL, 'E2E case 8',
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       -- Case 9: SUSPENDED, no validity end, no suspension end (permanent suspension).
       ('00000000-0000-0000-0000-0000000000c9',
        tstzrange(now() - interval '30 days', NULL, '[)'),
        tstzrange(now() - interval '5 days', NULL, '[)'),
        now() - interval '30 days',
        'INVESTIGATION', NULL, 'E2E case 9',
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       -- Case 10: SUSPENDED, no validity end, suspension with explicit end.
       ('00000000-0000-0000-0000-00000000000a',
        tstzrange(now() - interval '30 days', NULL, '[)'),
        tstzrange(now() - interval '5 days', now() + interval '30 days', '[)'),
        now() - interval '30 days',
        'INVESTIGATION', NULL, 'E2E case 10',
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       -- Case 11: SUSPENDED, validity end > 15 days.
       ('00000000-0000-0000-0000-00000000000b',
        tstzrange(now() - interval '30 days', now() + interval '30 days', '[)'),
        tstzrange(now() - interval '5 days', now() + interval '20 days', '[)'),
        now() - interval '30 days',
        'INVESTIGATION', NULL, 'E2E case 11',
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       -- Case 12: SUSPENDED, validity end <= 15 days.
       ('00000000-0000-0000-0000-00000000000c',
        tstzrange(now() - interval '30 days', now() + interval '10 days', '[)'),
        tstzrange(now() - interval '5 days', now() + interval '9 days', '[)'),
        now() - interval '30 days',
        'INVESTIGATION', NULL, 'E2E case 12',
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001')
ON CONFLICT (act_id) DO NOTHING;

-- One account per dialog group, with deterministic UUIDs for e2e targeting.
-- Each account exposes the minimum required state to trigger the listed dialogs.
INSERT
INTO accounts (act_id, external_id, email, lastname, firstname, payload,
               checksum, created_by, updated_by)
VALUES ('00000000-0000-4000-8000-0000000000d1', 'dialog-d1',
        'dialog-d1@example.com', 'ActivationDialogs', 'Inactive',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-0000000000d2', 'dialog-d2',
        'dialog-d2@example.com', 'SuspDeactDialogs', 'Active',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-0000-0000-00000000a001',
        '00000000-0000-0000-0000-00000000a001'),
       ('00000000-0000-4000-8000-0000000000d5', 'dialog-d5',
        'dialog-d5@example.com', 'ActScheduledDialog', 'Inactive',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-0000000000d8', 'dialog-d8',
        'dialog-d8@example.com', 'SuspScheduled', 'Active',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-0000000000d9', 'dialog-d9',
        'dialog-d9@example.com', 'ModifySusp', 'Suspended',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001')
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
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       -- D2: ACTIVE, no end date, no suspension.
       -- Unlocks: suspension.immediate, suspension.scheduled, deactivation.immediate, deactivation.scheduled
       ('00000000-0000-4000-8000-0000000000d2',
        tstzrange(now() - interval '30 days', NULL, '[)'),
        NULL,
        now() - interval '30 days',
        'Reason1', 'Subreason1', 'Dialog test D2: suspension/deactivation immediate dialogs',
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       -- D5: INACTIVE, validity start in the future.
       -- Unlocks: activation.immediate, activation.scheduled, suspension.scheduled, deactivation.scheduled
       ('00000000-0000-4000-8000-0000000000d5',
        tstzrange(now() + interval '30 days', NULL, '[)'),
        NULL,
        NULL,
        'Reason1', 'Subreason1', 'Dialog test D5: activation.scheduled dialog',
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       -- D8: ACTIVE, no end date, no suspension.
       -- Unlocks: suspension.scheduled
       ('00000000-0000-4000-8000-0000000000d8',
        tstzrange(now() - interval '30 days', NULL, '[)'),
        NULL,
        now() - interval '30 days',
        'Reason1', 'Subreason1', 'Dialog test D8: suspension.scheduled dialog',
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       -- D9: SUSPENDED, suspension period with both start and end defined.
       -- Unlocks: suspension.modify (AccountSuspendedBanner)
       ('00000000-0000-4000-8000-0000000000d9',
        tstzrange(now() - interval '30 days', NULL, '[)'),
        tstzrange(now() - interval '10 days', now() + interval '20 days', '[)'),
        now() - interval '30 days',
        'Reason1', 'Subreason1', 'Dialog test D9: modify suspension dialog',
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001')
ON CONFLICT (act_id) DO NOTHING;
