-- All users are created by account admin. The admin row uses a deterministic
-- UUID so other rows can reference it directly through created_by / updated_by.
INSERT
INTO accounts (act_id, external_id, email, lastname, firstname, payload,
               checksum, created_by, updated_by)
VALUES ('00000000-0000-4000-8000-00000000a001', 'admin',
        'admin@example.com', 'admin_ln', 'admin_fn',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-00000000a002', 'user1', 'user1@example.com', 'user1_ln', 'user1_fn',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-00000000a003', 'user2', 'user2@example.com', 'user2_ln', 'user2_fn',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-00000000a004', 'user3', 'user3@example.com', 'user3_ln', 'user3_fn',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-00000000a005', 'user4', 'user4@example.com', 'user4_ln', 'user4_fn',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-00000000a006', 'user5', 'user5@example.com', 'user5_ln', 'user5_fn',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-00000000a007', 'user6', 'user6@example.com', 'user6_ln', 'user6_fn',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-00000000a008', 'user7', 'user7@example.com', 'user7_ln', 'user7_fn',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-00000000a009', 'user8', 'user8@example.com', 'user8_ln', 'user8_fn',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-00000000a010', 'user9', 'user9@example.com', 'user9_ln', 'user9_fn',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-00000000a011', 'user10', 'user10@example.com', 'user10_ln', 'user10_fn',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001')
ON CONFLICT (email) DO NOTHING;

-- Lifecycle test accounts. Each row covers one case of the lifecycle UI
-- matrix from issue #112. UUIDs are deterministic so that e2e scenarios can
-- target them directly through /accounts/{id}. account_status rows below are
-- expressed relatively to now() so the cases stay valid regardless of when
-- the test environment is rebuilt.
INSERT
INTO accounts (act_id, external_id, email, lastname, firstname, payload,
               checksum, created_by, updated_by)
VALUES ('00000000-0000-4000-8000-0000000000c1', 'lifecycle-c1',
        'lifecycle-c1@example.com', 'FutureStart', 'Inactive',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-0000000000c2', 'lifecycle-c2',
        'lifecycle-c2@example.com', 'NotActivated', 'Inactive',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-0000000000c3', 'lifecycle-c3',
        'lifecycle-c3@example.com', 'NoEnd', 'Active',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-0000000000c4', 'lifecycle-c4',
        'lifecycle-c4@example.com', 'EndFar', 'Active',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-0000000000c5', 'lifecycle-c5',
        'lifecycle-c5@example.com', 'EndSoon', 'Active',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-0000000000c6', 'lifecycle-c6',
        'lifecycle-c6@example.com', 'NoEndSuspending', 'Active',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-0000000000c7', 'lifecycle-c7',
        'lifecycle-c7@example.com', 'EndFarSuspending', 'Active',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-0000000000c8', 'lifecycle-c8',
        'lifecycle-c8@example.com', 'EndSoonSuspending', 'Active',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-0000000000c9', 'lifecycle-c9',
        'lifecycle-c9@example.com', 'NoEnd', 'Suspended',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-00000000000a', 'lifecycle-c10',
        'lifecycle-c10@example.com', 'WithSuspensionEnd', 'Suspended',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-00000000000b', 'lifecycle-c11',
        'lifecycle-c11@example.com', 'EndFar', 'Suspended',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-00000000000c', 'lifecycle-c12',
        'lifecycle-c12@example.com', 'EndSoon', 'Suspended',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001')
ON CONFLICT (email) DO NOTHING;

-- Account status for basic users (user1-5 only).
-- Admin is excluded to allow testing of 404 when no status account row exists yet.
INSERT
INTO account_status (act_id, validity_period, created_by, updated_by)
SELECT a.act_id,
       tstzrange('2024-01-01 00:00:00+00'::timestamptz, NULL, '[)'),
       '00000000-0000-4000-8000-00000000a001',
       '00000000-0000-4000-8000-00000000a001'
FROM accounts a
WHERE a.external_id IN ('user1', 'user2', 'user3', 'user4', 'user5',
                        'user6', 'user7', 'user8', 'user9', 'user10')
ON CONFLICT (act_id) DO NOTHING;

INSERT
INTO account_status (act_id, validity_period, suspension_period, activation_at,
                     suspension_reason, suspension_subreason, suspension_comment,
                     deactivation_reason, deactivation_subreason, deactivation_comment,
                     reactivation_comment,
                     created_by, updated_by)
VALUES
    -- Case 1: INACTIVE, validity start in the future, never activated.
    ('00000000-0000-4000-8000-0000000000c1',
     tstzrange(now() + interval '30 days', NULL, '[)'),
     NULL,
     NULL,
     NULL, NULL, NULL,
     'Deactivation Reason A', 'Deactivation Sub-reason A.1', 'E2E case 1',
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001'),
    -- Case 2: INACTIVE, validity already started but never activated.
    ('00000000-0000-4000-8000-0000000000c2',
     tstzrange(now() - interval '30 days', NULL, '[)'),
     NULL,
     NULL,
     NULL, NULL, NULL,
     'Deactivation Reason A', 'Deactivation Sub-reason A.1', 'E2E case 2',
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001'),
    -- Case 3: ACTIVE, no end date, no suspension.
    ('00000000-0000-4000-8000-0000000000c3',
     tstzrange(now() - interval '30 days', NULL, '[)'),
     NULL,
     now() - interval '30 days',
     NULL, NULL, NULL,
     'Deactivation Reason A', 'Deactivation Sub-reason A.1', 'E2E case 3',
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001'),
    -- Case 4: ACTIVE, end > 15 days, no suspension.
    ('00000000-0000-4000-8000-0000000000c4',
     tstzrange(now() - interval '30 days', now() + interval '30 days', '[)'),
     NULL,
     now() - interval '30 days',
     NULL, NULL, NULL,
     'Deactivation Reason A', 'Deactivation Sub-reason A.1', 'E2E case 4',
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001'),
    -- Case 5: ACTIVE, end <= 15 days, no suspension.
    ('00000000-0000-4000-8000-0000000000c5',
     tstzrange(now() - interval '30 days', now() + interval '10 days', '[)'),
     NULL,
     now() - interval '30 days',
     NULL, NULL, NULL,
     'Deactivation Reason A', 'Deactivation Sub-reason A.1', 'E2E case 5',
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001'),
    -- Case 6: ACTIVE, no end date, future suspension planned.
    ('00000000-0000-4000-8000-0000000000c6',
     tstzrange(now() - interval '30 days', NULL, '[)'),
     tstzrange(now() + interval '10 days', now() + interval '20 days', '[)'),
     now() - interval '30 days',
     'Suspension Reason A', 'Suspension Sub-reason A.1', 'E2E case 6',
     NULL, NULL, NULL,
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001'),
    -- Case 7: ACTIVE, end > 15 days, future suspension planned.
    ('00000000-0000-4000-8000-0000000000c7',
     tstzrange(now() - interval '30 days', now() + interval '30 days', '[)'),
     tstzrange(now() + interval '10 days', now() + interval '20 days', '[)'),
     now() - interval '30 days',
     NULL, NULL, NULL,
     'Deactivation Reason A', 'Deactivation Sub-reason A.1', 'E2E case 7',
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001'),
    -- Case 8: ACTIVE, end <= 15 days, future suspension planned.
    ('00000000-0000-4000-8000-0000000000c8',
     tstzrange(now() - interval '30 days', now() + interval '10 days', '[)'),
     tstzrange(now() + interval '5 days', now() + interval '8 days', '[)'),
     now() - interval '30 days',
     NULL, NULL, NULL,
     'Deactivation Reason A', 'Deactivation Sub-reason A.1', 'E2E case 8',
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001'),
    -- Case 9: SUSPENDED, no validity end, no suspension end (permanent suspension).
    ('00000000-0000-4000-8000-0000000000c9',
     tstzrange(now() - interval '30 days', NULL, '[)'),
     tstzrange(now() - interval '5 days', NULL, '[)'),
     now() - interval '30 days',
     'Suspension Reason A', 'Suspension Sub-reason A.1', 'E2E case 9',
     NULL, NULL, NULL,
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001'),
    -- Case 10: SUSPENDED, no validity end, suspension with explicit end.
    ('00000000-0000-4000-8000-00000000000a',
     tstzrange(now() - interval '30 days', NULL, '[)'),
     tstzrange(now() - interval '5 days', now() + interval '30 days', '[)'),
     now() - interval '30 days',
     'Suspension Reason A', 'Suspension Sub-reason A.1', 'E2E case 10',
     NULL, NULL, NULL,
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001'),
    -- Case 11: SUSPENDED, validity end > 15 days.
    ('00000000-0000-4000-8000-00000000000b',
     tstzrange(now() - interval '30 days', now() + interval '30 days', '[)'),
     tstzrange(now() - interval '5 days', now() + interval '20 days', '[)'),
     now() - interval '30 days',
     'Suspension Reason A', 'Suspension Sub-reason A.1', 'E2E case 11',
     NULL, NULL, NULL,
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001'),
    -- Case 12: SUSPENDED, validity end <= 15 days.
    ('00000000-0000-4000-8000-00000000000c',
     tstzrange(now() - interval '30 days', now() + interval '10 days', '[)'),
     tstzrange(now() - interval '5 days', now() + interval '9 days', '[)'),
     now() - interval '30 days',
     'Suspension Reason A', 'Suspension Sub-reason A.1', 'E2E case 12',
     NULL, NULL, NULL,
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001')
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
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-0000000000d3', 'dialog-d3',
        'dialog-d3@example.com', 'DeactImmediateDialog', 'Active',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-0000000000d4', 'dialog-d4',
        'dialog-d4@example.com', 'ReactivImmediate', 'Suspended',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-0000000000d5', 'dialog-d5',
        'dialog-d5@example.com', 'ActScheduledDialog', 'Inactive',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-0000000000d6', 'dialog-d6',
        'dialog-d6@example.com', 'DeactScheduled', 'Active',
        '{}'::jsonb, encode(digest('{}', 'sha256'), 'hex'),
        '00000000-0000-4000-8000-00000000a001',
        '00000000-0000-4000-8000-00000000a001'),
       ('00000000-0000-4000-8000-0000000000d7', 'dialog-d7',
        'dialog-d7@example.com', 'ModifyDeact', 'Active',
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
                     suspension_reason, suspension_subreason, suspension_comment,
                     deactivation_reason, deactivation_subreason, deactivation_comment,
                     reactivation_comment,
                     created_by, updated_by)
VALUES
    -- D1: INACTIVE, validity start in the future.
    -- Unlocks: activation.immediate, activation.scheduled, suspension.scheduled, deactivation.scheduled
    ('00000000-0000-4000-8000-0000000000d1',
     tstzrange(now() + interval '30 days', NULL, '[)'),
     NULL,
     NULL,
     NULL, NULL, NULL,
     'Deactivation Reason A', 'Deactivation Sub-reason A.1', 'Dialog test D1: activation dialogs',
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001'),
    -- D2: ACTIVE, no end date, no suspension.
    -- Unlocks: suspension.immediate, suspension.scheduled, deactivation.immediate, deactivation.scheduled
    ('00000000-0000-4000-8000-0000000000d2',
     tstzrange(now() - interval '30 days', NULL, '[)'),
     NULL,
     now() - interval '30 days',
     NULL, NULL, NULL,
     'Deactivation Reason A', 'Deactivation Sub-reason A.1', 'Dialog test D2: suspension/deactivation immediate dialogs',
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001'),
    -- D3: ACTIVE, no end date, no suspension.
    -- Unlocks: deactivation.immediate
    ('00000000-0000-4000-8000-0000000000d3',
     tstzrange(now() - interval '30 days', NULL, '[)'),
     NULL,
     now() - interval '30 days',
     NULL, NULL, NULL,
     'Deactivation Reason A', 'Deactivation Sub-reason A.1', 'Dialog test D3: deactivation.immediate dialog',
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001'),
    -- D4: SUSPENDED, no validity end, no suspension end.
    -- Unlocks: reactivation.immediate
    ('00000000-0000-4000-8000-0000000000d4',
     tstzrange(now() - interval '30 days', NULL, '[)'),
     tstzrange(now() - interval '5 days', NULL, '[)'),
     now() - interval '30 days',
     'Suspension Reason B', 'Suspension Sub-reason B.1', 'Dialog test D4: reactivation.immediate dialog',
     NULL, NULL, NULL,
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001'),
    -- D5: INACTIVE, validity start in the future.
    -- Unlocks: activation.immediate, activation.scheduled, suspension.scheduled, deactivation.scheduled
    ('00000000-0000-4000-8000-0000000000d5',
     tstzrange(now() + interval '30 days', NULL, '[)'),
     NULL,
     NULL,
     NULL, NULL, NULL,
     'Deactivation Reason A', 'Deactivation Sub-reason A.1', 'Dialog test D5: activation.scheduled dialog',
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001'),
    -- D6: ACTIVE, no end date, no suspension.
    -- Unlocks: deactivation.scheduled
    ('00000000-0000-4000-8000-0000000000d6',
     tstzrange(now() - interval '30 days', NULL, '[)'),
     NULL,
     now() - interval '30 days',
     NULL, NULL, NULL,
     'Deactivation Reason A', 'Deactivation Sub-reason A.1', 'Dialog test D6: deactivation.scheduled dialog',
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001'),
    -- D7: ACTIVE, validity end in 7 days, no suspension.
    -- Unlocks: deactivation.modify (AccountDeactivatedWarningBanner)
    ('00000000-0000-4000-8000-0000000000d7',
     tstzrange(now() - interval '30 days', now() + interval '7 days', '[)'),
     NULL,
     now() - interval '30 days',
     NULL, NULL, NULL,
     'Deactivation Reason A', 'Deactivation Sub-reason A.1', 'Dialog test D7: modify deactivation dialog',
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001'),
    -- D8: ACTIVE, no end date, no suspension.
    -- Unlocks: suspension.scheduled
    ('00000000-0000-4000-8000-0000000000d8',
     tstzrange(now() - interval '30 days', NULL, '[)'),
     NULL,
     now() - interval '30 days',
     NULL, NULL, NULL,
     'Deactivation Reason A', 'Deactivation Sub-reason A.1', 'Dialog test D8: suspension.scheduled dialog',
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001'),
    -- D9: SUSPENDED, suspension period with both start and end defined.
    -- Unlocks: suspension.modify (AccountSuspendedBanner)
    ('00000000-0000-4000-8000-0000000000d9',
     tstzrange(now() - interval '30 days', NULL, '[)'),
     tstzrange(now() - interval '10 days', now() + interval '20 days', '[)'),
     now() - interval '30 days',
     'Suspension Reason A', 'Suspension Sub-reason A.1', 'Dialog test D9: modify suspension dialog',
     NULL, NULL, NULL,
     NULL,
     '00000000-0000-4000-8000-00000000a001',
     '00000000-0000-4000-8000-00000000a001')
ON CONFLICT (act_id) DO NOTHING;

-- Create Organizational Unit tree
DO
$$
    DECLARE
        root_id          UUID;
        DECLARE admin_id UUID;
    BEGIN
        SELECT oun_id
        INTO root_id
        FROM organizational_units
        WHERE name = 'root'
        LIMIT 1;

        SELECT act_id
        INTO admin_id
        FROM accounts
        WHERE email = 'admin@example.com'
        LIMIT 1;

        -- =========================================================
        -- 1. LEVEL 1 - COMPANIES (direct children of root)
        -- =========================================================
        INSERT INTO organizational_units (oun_id, name, type, created_by, updated_by)
        VALUES ('00000000-0000-4000-8000-00000000000a', 'Company A', 'COMPANY', admin_id, admin_id),
               ('00000000-0000-4000-8000-00000000000b', 'Company B', 'COMPANY', admin_id, admin_id)
        ON CONFLICT (type, name) DO NOTHING;

        -- Link root to companies
        INSERT INTO organizational_unit_relations (our_id, parent_id, child_id, created_by, updated_by)
        VALUES ('00000000-0000-4000-8000-0000000000aa', root_id, '00000000-0000-4000-8000-00000000000a', admin_id,
                admin_id),
               ('00000000-0000-4000-8000-0000000000bb', root_id, '00000000-0000-4000-8000-00000000000b', admin_id,
                admin_id)
        ON CONFLICT (parent_id, child_id) DO NOTHING;

        -- =========================================================
        -- 2. LEVEL 2 - DIVISIONS
        -- =========================================================
        INSERT INTO organizational_units (oun_id, name, type, created_by, updated_by)
        VALUES ('00000000-0000-4000-8000-00000000000c', 'Division A1', 'DIVISION', admin_id, admin_id),
               ('00000000-0000-4000-8000-00000000000d', 'Division A2', 'DIVISION', admin_id, admin_id),
               ('00000000-0000-4000-8000-00000000000e', 'Division B1', 'DIVISION', admin_id, admin_id)
        ON CONFLICT (type, name) DO NOTHING;

        -- Link companies to divisions
        INSERT INTO organizational_unit_relations (our_id, parent_id, child_id, created_by, updated_by)
        VALUES ('00000000-0000-4000-8000-0000000000cc', '00000000-0000-4000-8000-00000000000a',
                '00000000-0000-4000-8000-00000000000c', admin_id, admin_id),
               ('00000000-0000-4000-8000-0000000000dd', '00000000-0000-4000-8000-00000000000a',
                '00000000-0000-4000-8000-00000000000d', admin_id, admin_id),
               ('00000000-0000-4000-8000-0000000000ee', '00000000-0000-4000-8000-00000000000b',
                '00000000-0000-4000-8000-00000000000e', admin_id, admin_id)
        ON CONFLICT (parent_id, child_id) DO NOTHING;

        -- =========================================================
        -- 3. LEVEL 3 - DEPARTMENTS
        -- =========================================================
        INSERT INTO organizational_units (oun_id, name, type, created_by, updated_by)
        VALUES ('00000000-0000-4000-8000-00000000000f', 'Dept A1-1', 'DEPARTMENT', admin_id, admin_id),
               ('00000000-0000-4000-8000-0000000000a1', 'Dept A1-2', 'DEPARTMENT', admin_id, admin_id),
               ('00000000-0000-4000-8000-0000000000b1', 'Dept A2-1', 'DEPARTMENT', admin_id, admin_id),
               ('00000000-0000-4000-8000-0000000000c1', 'Dept B1-1', 'DEPARTMENT', admin_id, admin_id)
        ON CONFLICT (type, name) DO NOTHING;

        -- Link divisions to departments
        INSERT INTO organizational_unit_relations (our_id, parent_id, child_id, created_by, updated_by)
        VALUES ('00000000-0000-4000-8000-0000000000ff', '00000000-0000-4000-8000-00000000000c',
                '00000000-0000-4000-8000-00000000000f', admin_id, admin_id),
               ('00000000-0000-4000-8000-000000000aa1', '00000000-0000-4000-8000-00000000000c',
                '00000000-0000-4000-8000-0000000000a1', admin_id, admin_id),
               ('00000000-0000-4000-8000-000000000bb1', '00000000-0000-4000-8000-00000000000d',
                '00000000-0000-4000-8000-0000000000b1', admin_id, admin_id),
               ('00000000-0000-4000-8000-000000000cc1', '00000000-0000-4000-8000-00000000000e',
                '00000000-0000-4000-8000-0000000000c1', admin_id, admin_id)
        ON CONFLICT (parent_id, child_id) DO NOTHING;

        -- =========================================================
        -- 4. LEVEL 4 - TEAMS
        -- =========================================================
        INSERT INTO organizational_units (oun_id, name, type, created_by, updated_by)
        VALUES ('00000000-0000-4000-8000-000000000dd1', 'Team Alpha', 'TEAM', admin_id, admin_id),
               ('00000000-0000-4000-8000-000000000ee1', 'Team Beta', 'TEAM', admin_id, admin_id),
               ('00000000-0000-4000-8000-000000000ff1', 'Team Gamma', 'TEAM', admin_id, admin_id),
               ('00000000-0000-4000-8000-00000000fff1', 'Team Delta', 'TEAM', admin_id, admin_id)
        ON CONFLICT (type, name) DO NOTHING;

        -- Link departments to teams
        INSERT INTO organizational_unit_relations (our_id, parent_id, child_id, created_by, updated_by)
        VALUES ('00000000-0000-4000-8000-00000000ddd1', '00000000-0000-4000-8000-00000000000f',
                '00000000-0000-4000-8000-000000000dd1', admin_id, admin_id),
               ('00000000-0000-4000-8000-00000000eee1', '00000000-0000-4000-8000-0000000000a1',
                '00000000-0000-4000-8000-000000000ee1', admin_id, admin_id),
               ('00000000-0000-4000-8000-0000000ffff1', '00000000-0000-4000-8000-0000000000b1',
                '00000000-0000-4000-8000-000000000ff1', admin_id, admin_id),
               ('00000000-0000-4000-8000-00000000dff1', '00000000-0000-4000-8000-0000000000c1',
                '00000000-0000-4000-8000-00000000fff1', admin_id, admin_id)
        ON CONFLICT (parent_id, child_id) DO NOTHING;

        -- =========================================================
        -- 5. MULTI-PARENT RELATIONS (DAG stress test cases)
        -- =========================================================

        -- Team Gamma has two parents (multi-parent scenario)
        INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
        SELECT dep.oun_id, t.oun_id, admin_id, admin_id
        FROM organizational_units dep
                 JOIN organizational_units t ON TRUE
        WHERE dep.name = 'Dept A1-2'
          AND t.name = 'Team Gamma'
        ON CONFLICT (parent_id, child_id) DO NOTHING;

        -- Team Delta has two parents (multi-parent scenario)
        INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
        SELECT dep.oun_id, t.oun_id, admin_id, admin_id
        FROM organizational_units dep
                 JOIN organizational_units t ON TRUE
        WHERE dep.name = 'Dept A2-1'
          AND t.name = 'Team Delta'
        ON CONFLICT (parent_id, child_id) DO NOTHING;


        -- =========================================================
        -- 6. Add user inside OU
        -- =========================================================
        -- Admin in OU root
        INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
        VALUES (root_id, admin_id, admin_id, admin_id);

        -- user1 in OU Company A
        INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
        VALUES ((SELECT oun_id FROM organizational_units WHERE name = 'Company A'),
                (SELECT act_id FROM accounts WHERE external_id = 'user1'),
                admin_id,
                admin_id);

        -- user2 in OU Company B
        INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
        VALUES ((SELECT oun_id FROM organizational_units WHERE name = 'Company B'),
                (SELECT act_id FROM accounts WHERE external_id = 'user2'),
                admin_id,
                admin_id);

        -- user3 in OU Division A1
        INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
        VALUES ((SELECT oun_id FROM organizational_units WHERE name = 'Division A1'),
                (SELECT act_id FROM accounts WHERE external_id = 'user3'),
                admin_id,
                admin_id);

        -- user4 in OU Division A2
        INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
        VALUES ((SELECT oun_id FROM organizational_units WHERE name = 'Division A2'),
                (SELECT act_id FROM accounts WHERE external_id = 'user4'),
                admin_id,
                admin_id);

        -- Insert all users in OU Team Beta
        INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
        SELECT (SELECT oun_id FROM organizational_units WHERE name = 'Team Beta'),
               a.act_id,
               admin_id,
               admin_id
        FROM accounts a
        WHERE a.external_id IN (
                                'user1', 'user2', 'user3', 'user4', 'user5',
                                'user6', 'user7', 'user8', 'user9', 'user10'
            );

        -- Insert all lifecycle and dialog users in OU Team Alpha
        INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
        SELECT (SELECT oun_id FROM organizational_units WHERE name = 'Team Alpha'),
               a.act_id,
               admin_id,
               admin_id
        FROM accounts a
        WHERE a.external_id IN (
                                'lifecycle-c1', 'lifecycle-c2', 'lifecycle-c3', 'lifecycle-c4', 'lifecycle-c5',
                                'lifecycle-c6', 'lifecycle-c7', 'lifecycle-c8', 'lifecycle-c9', 'lifecycle-c10',
                                'lifecycle-c11', 'lifecycle-c12',
                                'dialog-d1', 'dialog-d2', 'dialog-d3', 'dialog-d4', 'dialog-d5', 'dialog-d6',
                                'dialog-d7', 'dialog-d8', 'dialog-d9'
            );


        -- user5 in OU Division B1
        INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
        VALUES ((SELECT oun_id FROM organizational_units WHERE name = 'Division B1'),
                (SELECT act_id FROM accounts WHERE external_id = 'user5'),
                admin_id,
                admin_id);

        -- =========================================================
        -- 7. SUSPENDED ORGANIZATIONAL UNITS
        -- Currently-suspended OUs (one open-ended, one with an end date).
        -- suspension_period is expressed relatively to now() so the cases
        -- stay valid regardless of when the environment is rebuilt.
        -- =========================================================

        -- Suspended OU without end date (permanent suspension)
        INSERT INTO organizational_units (oun_id, name, type, created_by, updated_by)
        VALUES ('00000000-0000-4000-8000-0000000000e1', 'SuspendedOuNoEnd', 'COMPANY', admin_id, admin_id)
        ON CONFLICT (type, name) DO NOTHING;

        INSERT INTO organizational_unit_relations (our_id, parent_id, child_id, created_by, updated_by)
        VALUES ('00000000-0000-4000-8000-0000000000e2', root_id,
                '00000000-0000-4000-8000-0000000000e1', admin_id, admin_id)
        ON CONFLICT (parent_id, child_id) DO NOTHING;

        UPDATE organizational_unit_status
        SET suspension_period = tstzrange(now() - interval '5 days', NULL, '[)'),
            suspension_reason     = 'OU Suspension Reason',
            suspension_subreason  = 'OU Suspension Sub-reason',
            suspension_comment    = 'Suspended OU without end date'
        WHERE oun_id = '00000000-0000-4000-8000-0000000000e1';

        -- Suspended OU with an end date
        INSERT INTO organizational_units (oun_id, name, type, created_by, updated_by)
        VALUES ('00000000-0000-4000-8000-0000000000e3', 'SuspendedOuWithEnd', 'COMPANY', admin_id, admin_id)
        ON CONFLICT (type, name) DO NOTHING;

        INSERT INTO organizational_unit_relations (our_id, parent_id, child_id, created_by, updated_by)
        VALUES ('00000000-0000-4000-8000-0000000000e4', root_id,
                '00000000-0000-4000-8000-0000000000e3', admin_id, admin_id)
        ON CONFLICT (parent_id, child_id) DO NOTHING;

        UPDATE organizational_unit_status
        SET suspension_period = tstzrange(now() - interval '5 days', now() + interval '30 days', '[)'),
            suspension_reason     = 'OU Suspension Reason',
            suspension_subreason  = 'OU Suspension Sub-reason',
            suspension_comment    = 'Suspended OU with end date'
        WHERE oun_id = '00000000-0000-4000-8000-0000000000e3';

    END
$$;
