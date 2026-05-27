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

-- Create Organizational Unit tree
DO $$
    DECLARE root_id UUID;
        DECLARE admin_id UUID;
    BEGIN
        SELECT oun_id INTO root_id
        FROM organizational_units
        WHERE name = 'root'
        LIMIT 1;

        SELECT act_id INTO admin_id
        FROM accounts
        WHERE email = 'admin@example.com'
        LIMIT 1;

        -- =========================================================
        -- 1. LEVEL 1 - COMPANIES (direct children of root)
        -- =========================================================
        INSERT INTO organizational_units (oun_id, name, type, created_by, updated_by)
        VALUES
            (gen_random_uuid(), 'Company A', 'COMPANY', admin_id, admin_id),
            (gen_random_uuid(), 'Company B', 'COMPANY', admin_id, admin_id)
        ON CONFLICT (type, name) DO NOTHING;

        -- Link root to companies
        INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
        SELECT root_id, oun_id, admin_id, admin_id
        FROM organizational_units
        WHERE type = 'COMPANY'
        ON CONFLICT (parent_id, child_id) DO NOTHING;

        -- =========================================================
        -- 2. LEVEL 2 - DIVISIONS
        -- =========================================================
        INSERT INTO organizational_units (oun_id, name, type, created_by, updated_by)
        VALUES
            (gen_random_uuid(), 'Division A1', 'DIVISION', admin_id, admin_id),
            (gen_random_uuid(), 'Division A2', 'DIVISION', admin_id, admin_id),
            (gen_random_uuid(), 'Division B1', 'DIVISION', admin_id, admin_id)
        ON CONFLICT (type, name) DO NOTHING;

        -- Link Company A to its divisions
        INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
        SELECT c.oun_id, d.oun_id, admin_id, admin_id
        FROM organizational_units c
                 JOIN organizational_units d ON TRUE
        WHERE c.name = 'Company A'
          AND c.type = 'COMPANY'
          AND d.type = 'DIVISION'
          AND d.name IN ('Division A1', 'Division A2')
        ON CONFLICT (parent_id, child_id) DO NOTHING;

        -- Link Company B to its division
        INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
        SELECT c.oun_id, d.oun_id, admin_id, admin_id
        FROM organizational_units c
                 JOIN organizational_units d ON TRUE
        WHERE c.name = 'Company B'
          AND c.type = 'COMPANY'
          AND d.type = 'DIVISION'
          AND d.name = 'Division B1'
        ON CONFLICT (parent_id, child_id) DO NOTHING;

        -- =========================================================
        -- 3. LEVEL 3 - DEPARTMENTS
        -- =========================================================
        INSERT INTO organizational_units (oun_id, name, type, created_by, updated_by)
        VALUES
            (gen_random_uuid(), 'Dept A1-1', 'DEPARTMENT', admin_id, admin_id),
            (gen_random_uuid(), 'Dept A1-2', 'DEPARTMENT', admin_id, admin_id),
            (gen_random_uuid(), 'Dept A2-1', 'DEPARTMENT', admin_id, admin_id),
            (gen_random_uuid(), 'Dept B1-1', 'DEPARTMENT', admin_id, admin_id)
        ON CONFLICT (type, name) DO NOTHING;

        -- Link divisions to departments
        INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
        SELECT d.oun_id, dep.oun_id, admin_id, admin_id
        FROM organizational_units d
                 JOIN organizational_units dep ON TRUE
        WHERE d.name = 'Division A1'
          AND dep.name IN ('Dept A1-1', 'Dept A1-2')
        ON CONFLICT (parent_id, child_id) DO NOTHING;

        INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
        SELECT d.oun_id, dep.oun_id, admin_id, admin_id
        FROM organizational_units d
                 JOIN organizational_units dep ON TRUE
        WHERE d.name = 'Division A2'
          AND dep.name = 'Dept A2-1'
        ON CONFLICT (parent_id, child_id) DO NOTHING;

        INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
        SELECT d.oun_id, dep.oun_id, admin_id, admin_id
        FROM organizational_units d
                 JOIN organizational_units dep ON TRUE
        WHERE d.name = 'Division B1'
          AND dep.name = 'Dept B1-1'
        ON CONFLICT (parent_id, child_id) DO NOTHING;

        -- =========================================================
        -- 4. LEVEL 4 - TEAMS
        -- =========================================================
        INSERT INTO organizational_units (oun_id, name, type, created_by, updated_by)
        VALUES
            (gen_random_uuid(), 'Team Alpha', 'TEAM', admin_id, admin_id),
            (gen_random_uuid(), 'Team Beta', 'TEAM', admin_id, admin_id),
            (gen_random_uuid(), 'Team Gamma', 'TEAM', admin_id, admin_id),
            (gen_random_uuid(), 'Team Delta', 'TEAM', admin_id, admin_id)
        ON CONFLICT (type, name) DO NOTHING;

        -- Link departments to teams
        INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
        SELECT dep.oun_id, t.oun_id, admin_id, admin_id
        FROM organizational_units dep
                 JOIN organizational_units t ON TRUE
        WHERE (dep.name = 'Dept A1-1' AND t.name = 'Team Alpha')
           OR (dep.name = 'Dept A1-2' AND t.name = 'Team Beta')
           OR (dep.name = 'Dept A2-1' AND t.name = 'Team Gamma')
           OR (dep.name = 'Dept B1-1' AND t.name = 'Team Delta')
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
        VALUES (
                   (SELECT oun_id FROM organizational_units WHERE name = 'Company A'),
                   (SELECT act_id FROM accounts WHERE external_id = 'user1'),
                   admin_id,
                   admin_id
               );

        -- user2 in OU Company B
        INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
        VALUES (
                   (SELECT oun_id FROM organizational_units WHERE name = 'Company B'),
                   (SELECT act_id FROM accounts WHERE external_id = 'user2'),
                   admin_id,
                   admin_id
               );

        -- user3 in OU Division A1
        INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
        VALUES (
                   (SELECT oun_id FROM organizational_units WHERE name = 'Division A1'),
                   (SELECT act_id FROM accounts WHERE external_id = 'user3'),
                   admin_id,
                   admin_id
               );

        -- user4 in OU Division A2
        INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
        VALUES (
                   (SELECT oun_id FROM organizational_units WHERE name = 'Division A2'),
                   (SELECT act_id FROM accounts WHERE external_id = 'user4'),
                   admin_id,
                   admin_id
               );

        -- user5 in OU Division B1
        INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
        VALUES (
                   (SELECT oun_id FROM organizational_units WHERE name = 'Division B1'),
                   (SELECT act_id FROM accounts WHERE external_id = 'user5'),
                   admin_id,
                   admin_id
               );

    END $$;
